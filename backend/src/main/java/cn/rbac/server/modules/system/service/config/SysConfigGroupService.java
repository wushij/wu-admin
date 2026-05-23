package cn.rbac.server.modules.system.service.config;

import cn.rbac.server.modules.system.dal.dataobject.config.SysConfigGroupDO;

import java.util.List;

public interface SysConfigGroupService {

    List<SysConfigGroupDO> listAll();

    SysConfigGroupDO getByGroupCode(String groupCode);

    void updateConfig(String groupCode, String configValue);
}
