package com.admin.server.modules.system.service.operlog;

import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.dal.dataobject.operlog.OperLogDO;

public interface OperLogService {

    PageResult<OperLogDO> page(Integer pageNo, Integer pageSize, String title, String operName, Integer status);

    void recordLog(OperLogDO operLog);

    void delete(Long id);

    void clean();
}
