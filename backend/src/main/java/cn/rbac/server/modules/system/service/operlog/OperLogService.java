package cn.rbac.server.modules.system.service.operlog;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.operlog.OperLogDO;

public interface OperLogService {

    PageResult<OperLogDO> page(Integer pageNo, Integer pageSize, String title, String operName, Integer status);

    void recordLog(OperLogDO operLog);

    void delete(Long id);

    void clean();
}
