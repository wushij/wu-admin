package com.admin.server.modules.infra.service.file.impl;

import com.admin.server.modules.infra.dal.dataobject.file.SysFileDO;
import com.admin.server.modules.infra.dal.dataobject.file.SysFileGroupDO;
import com.admin.server.modules.infra.dal.mysql.file.SysFileGroupMapper;
import com.admin.server.modules.infra.dal.mysql.file.SysFileMapper;
import com.admin.server.modules.infra.service.file.SysFileGroupService;
import com.admin.server.modules.infra.service.file.SysFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysFileGroupServiceImpl extends ServiceImpl<SysFileGroupMapper, SysFileGroupDO>
        implements SysFileGroupService {

    @Resource
    private SysFileMapper fileMapper;

    @Resource
    private SysFileService fileService;

    @Override
    public Map<String, Object> listWithUngroupedCount(String fileCategory) {
        List<SysFileGroupDO> groups = list(new LambdaQueryWrapper<SysFileGroupDO>()
                .orderByAsc(SysFileGroupDO::getSort)
                .orderByAsc(SysFileGroupDO::getId));
        for (SysFileGroupDO group : groups) {
            group.setFileCount((int) fileService.countForGroupSidebar(group.getId(), false, fileCategory));
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
        save(group);
    }

    @Override
    public void update(SysFileGroupDO group) {
        updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        fileMapper.update(null, new LambdaUpdateWrapper<SysFileDO>()
                .set(SysFileDO::getGroupId, null)
                .eq(SysFileDO::getGroupId, id));
        removeById(id);
    }
}
