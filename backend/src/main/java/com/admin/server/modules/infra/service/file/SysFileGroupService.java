package com.admin.server.modules.infra.service.file;

import com.admin.server.modules.infra.dal.dataobject.file.SysFileGroupDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface SysFileGroupService extends IService<SysFileGroupDO> {

    Map<String, Object> listWithUngroupedCount(String fileCategory);

    void create(SysFileGroupDO group);

    void update(SysFileGroupDO group);

    void delete(Long id);
}
