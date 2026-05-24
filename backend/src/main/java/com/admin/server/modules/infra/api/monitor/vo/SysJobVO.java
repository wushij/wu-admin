package com.admin.server.modules.infra.api.monitor.vo;

import com.admin.server.modules.infra.dal.dataobject.job.SysJobDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysJobVO extends SysJobDO {
    /** 下次执行时间（已暂停则为空） */
    private String nextFireTime;
    /** Cron 可读说明 */
    private String cronHint;
}
