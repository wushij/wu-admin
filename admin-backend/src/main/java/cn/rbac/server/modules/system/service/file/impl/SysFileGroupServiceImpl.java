package cn.rbac.server.modules.system.service.file.impl;

import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileGroupDO;
import cn.rbac.server.modules.system.dal.mysql.file.SysFileGroupMapper;
import cn.rbac.server.modules.system.dal.mysql.file.SysFileMapper;
import cn.rbac.server.modules.system.service.file.SysFileGroupService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysFileGroupServiceImpl extends ServiceImpl<SysFileGroupMapper, SysFileGroupDO>
        implements SysFileGroupService {

    @Resource
    private SysFileMapper fileMapper;

    @Override
    public Map<String, Object> listWithUngroupedCount() {
        List<SysFileGroupDO> groups = baseMapper.selectListWithFileCount();
        Integer ungroupedCount = baseMapper.selectUngroupedFileCount();
        Map<String, Object> result = new HashMap<>();
        result.put("groups", groups);
        result.put("ungroupedCount", ungroupedCount == null ? 0 : ungroupedCount);
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
