package cn.rbac.server.framework.quartz.util;

import cn.rbac.server.modules.system.dal.dataobject.job.SysJobDO;
import org.quartz.JobExecutionContext;

public class QuartzJobExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, SysJobDO job) throws Exception {
        JobInvokeUtil.invokeMethod(job);
    }
}
