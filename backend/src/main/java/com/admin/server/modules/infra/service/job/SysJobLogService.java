package com.admin.server.modules.infra.service.job;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.infra.dal.dataobject.job.SysJobLogDO;

import java.util.Map;

public interface SysJobLogService {
    void recordLog(SysJobLogDO log);

    PageResult<SysJobLogDO> page(Integer pageNo, Integer pageSize, String jobName, String jobGroup, Integer status);

    Map<String, Object> statistics();

    void cleanAll();

    void cleanScope(String jobName, String jobGroup);

    PageResult<SysJobLogDO> recyclePage(com.admin.server.common.core.PageParam pageParam, String jobName, String jobGroup);

    void restore(Long id);

    void deletePermanent(Long id);

    /** 物理清理早于指定天数的调度日志（定时任务保留策略） */
    int cleanOlderThan(int days);
}
