package cn.rbac.server.modules.system.service.job;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.job.SysJobLogDO;

import java.util.Map;

public interface SysJobLogService {
    void recordLog(SysJobLogDO log);

    PageResult<SysJobLogDO> page(Integer pageNo, Integer pageSize, String jobName, String jobGroup, Integer status);

    Map<String, Object> statistics();

    void clean();

    /** 清理早于指定天数的调度日志 */
    int cleanOlderThan(int days);
}
