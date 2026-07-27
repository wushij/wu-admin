package com.admin.server.framework.quartz.util;

import com.admin.server.modules.infra.dal.dataobject.job.SysJobDO;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, SysJobDO job) throws Exception {
        JobInvokeUtil.invokeMethod(job);
    }
}
