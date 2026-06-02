package cn.rbac.server.modules.system.service.file;

import cn.rbac.server.modules.system.dal.dataobject.file.SysFileGroupDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface SysFileGroupService extends IService<SysFileGroupDO> {

    Map<String, Object> listWithUngroupedCount();

    void create(SysFileGroupDO group);

    void update(SysFileGroupDO group);

    void delete(Long id);
}
