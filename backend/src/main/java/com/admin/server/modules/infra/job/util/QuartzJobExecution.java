package com.admin.server.modules.infra.job.util;

import com.admin.server.modules.infra.dal.dataobject.job.SysJobDO;
import org.quartz.JobExecutionContext;

public class QuartzJobExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, SysJobDO job) throws Exception {
        JobInvokeUtil.invokeMethod(job);
    }
}
