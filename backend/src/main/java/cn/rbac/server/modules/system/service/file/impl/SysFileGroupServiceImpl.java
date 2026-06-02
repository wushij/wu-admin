package cn.rbac.server.modules.system.service.file.impl;

import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileGroupDO;
import cn.rbac.server.modules.system.dal.mysql.file.SysFileGroupMapper;
import cn.rbac.server.modules.system.dal.mysql.file.SysFileMapper;
import cn.rbac.server.modules.system.service.file.SysFileGroupService;
import cn.rbac.server.modules.system.service.file.SysFileService;
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
