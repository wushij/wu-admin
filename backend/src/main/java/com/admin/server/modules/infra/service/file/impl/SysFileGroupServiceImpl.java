package com.admin.server.modules.infra.service.file.impl;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.infra.dal.dataobject.file.SysFileDO;
import com.admin.server.modules.infra.dal.dataobject.file.SysFileGroupDO;
import com.admin.server.modules.infra.dal.mysql.file.SysFileGroupMapper;
import com.admin.server.modules.infra.dal.mysql.file.SysFileMapper;
import com.admin.server.modules.infra.service.file.SysFileGroupService;
import com.admin.server.modules.infra.service.file.SysFileService;
import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.modules.system.service.permission.DataScopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class SysFileGroupServiceImpl extends ServiceImpl<SysFileGroupMapper, SysFileGroupDO>
        implements SysFileGroupService {

    @Resource
    private SysFileMapper fileMapper;

    @Resource
    private SysFileService fileService;

    @Resource
    private DataScopeService dataScopeService;

    @Override
    public Map<String, Object> listWithUngroupedCount(String fileCategory) {
        LambdaQueryWrapper<SysFileGroupDO> wrapper = new LambdaQueryWrapper<SysFileGroupDO>()
                .orderByAsc(SysFileGroupDO::getSort)
                .orderByAsc(SysFileGroupDO::getId);
        dataScopeService.applyDeptIdScope(wrapper, SysFileGroupDO::getDeptId);
        List<SysFileGroupDO> groups = list(wrapper);
        List<Long> groupIds = groups.stream()
                .map(SysFileGroupDO::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Integer> countMap = new HashMap<>();
        if (!groupIds.isEmpty()) {
            // 一次聚合查询替代逐个分组 count，避免分组多时产生 N+1
            DataScopeService.DataScopeContext scope = dataScopeService.getCurrentScope();
            List<Long> deptIds = scope.all() ? null : scope.deptIds();
            String selfCreateBy = scope.selfOnly() ? SecurityUtils.getLoginUsername() : null;
            for (Map<String, Object> row : fileMapper.countGroupedFiles(
                    groupIds, deptIds, selfCreateBy,
                    resolveFileTypePrefix(fileCategory),
                    "other".equalsIgnoreCase(fileCategory))) {
                Object groupId = row.get("groupId");
                Object total = row.get("total");
                if (groupId instanceof Number gid && total instanceof Number cnt) {
                    countMap.put(gid.longValue(), cnt.intValue());
                }
            }
        }
        for (SysFileGroupDO group : groups) {
            group.setFileCount(countMap.getOrDefault(group.getId(), 0));
        }
        long ungroupedCount = fileService.countForGroupSidebar(null, true, fileCategory);
        Map<String, Object> result = new HashMap<>();
        result.put("groups", groups);
        result.put("ungroupedCount", ungroupedCount);
        return result;
    }

    @Override
    public void create(SysFileGroupDO group) {
        if (group.getSort() == null) {
            group.setSort(0);
        }
        group.setDeptId(dataScopeService.getCurrentDeptId());
        save(group);
    }

    @Override
    public void update(SysFileGroupDO group) {
        requireVisibleGroup(group.getId());
        // 不允许通过请求体把分组迁移到其它部门
        group.setDeptId(null);
        updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireVisibleGroup(id);
        fileMapper.update(null, new LambdaUpdateWrapper<SysFileDO>()
                .set(SysFileDO::getGroupId, null)
                .eq(SysFileDO::getGroupId, id));
        removeById(id);
    }

    private String resolveFileTypePrefix(String fileCategory) {
        if (!StringUtils.hasText(fileCategory) || "all".equalsIgnoreCase(fileCategory)) {
            return null;
        }
        return switch (fileCategory.toLowerCase(Locale.ROOT)) {
            case "image" -> "image/";
            case "video" -> "video/";
            case "audio" -> "audio/";
            default -> null;
        };
    }

    private SysFileGroupDO requireVisibleGroup(Long id) {
        if (id == null) {
            throw new BusinessException(400, "分组 ID 不能为空");
        }
        SysFileGroupDO group = getById(id);
        if (group == null) {
            throw new BusinessException(404, "文件分组不存在");
        }
        DataScopeService.DataScopeContext scope = dataScopeService.getCurrentScope();
        if (scope.all()) {
            return group;
        }
        if (group.getDeptId() != null && scope.deptIds().contains(group.getDeptId())) {
            return group;
        }
        if (scope.selfOnly()) {
            String username = SecurityUtils.getLoginUsername();
            if (StringUtils.hasText(username) && Objects.equals(group.getCreateBy(), username)) {
                return group;
            }
        }
        throw new BusinessException(403, "无权操作该文件分组");
    }
}
