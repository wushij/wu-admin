package cn.rbac.server.framework.quartz.util;

import cn.rbac.server.modules.system.dal.dataobject.job.SysJobDO;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, SysJobDO job) throws Exception {
        JobInvokeUtil.invokeMethod(job);
    }
}
