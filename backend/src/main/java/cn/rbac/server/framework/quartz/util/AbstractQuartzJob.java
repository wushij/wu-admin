package cn.rbac.server.framework.quartz.util;

import cn.rbac.server.modules.system.dal.dataobject.job.SysJobDO;
import cn.rbac.server.modules.system.dal.dataobject.job.SysJobLogDO;
import cn.rbac.server.modules.system.service.job.SysJobLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
public abstract class AbstractQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        SysJobDO job = (SysJobDO) context.getMergedJobDataMap().get(ScheduleUtils.TASK_PROPERTIES);
        SysJobLogDO jobLog = new SysJobLogDO();
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setStartTime(LocalDateTime.now());

        try {
            doExecute(context, job);
            after(jobLog, null);
        } catch (Exception e) {
            log.error("定时任务执行异常：{}", job.getJobName(), e);
            after(jobLog, e);
        }
    }

    protected abstract void doExecute(JobExecutionContext context, SysJobDO job) throws Exception;

    private void after(SysJobLogDO jobLog, Exception e) {
        jobLog.setStopTime(LocalDateTime.now());
        if (e != null) {
            jobLog.setStatus(1);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            String errorMsg = sw.toString();
            jobLog.setExceptionInfo(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
            jobLog.setJobMessage("执行失败");
        } else {
            jobLog.setStatus(0);
            jobLog.setJobMessage("执行成功");
        }
        try {
            SpringUtils.getBean(SysJobLogService.class).recordLog(jobLog);
        } catch (Exception ex) {
            log.error("保存任务日志失败", ex);
        }
    }
}
