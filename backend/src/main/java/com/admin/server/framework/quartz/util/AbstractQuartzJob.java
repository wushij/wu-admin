package com.admin.server.framework.quartz.util;

import com.admin.server.modules.infra.dal.dataobject.job.SysJobDO;
import com.admin.server.modules.infra.dal.dataobject.job.SysJobLogDO;
import com.admin.server.modules.infra.service.job.SysJobLogService;
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
        long startMs = System.currentTimeMillis();
        jobLog.setStartTime(LocalDateTime.now());

        try {
            doExecute(context, job);
            after(jobLog, startMs, null);
        } catch (Exception e) {
            log.error("定时任务执行异常：{}", job.getJobName(), e);
            after(jobLog, startMs, e);
        }
    }

    protected abstract void doExecute(JobExecutionContext context, SysJobDO job) throws Exception;

    private void after(SysJobLogDO jobLog, long startMs, Exception e) {
        jobLog.setStopTime(LocalDateTime.now());
        jobLog.setDurationMs(Math.max(0L, System.currentTimeMillis() - startMs));
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
