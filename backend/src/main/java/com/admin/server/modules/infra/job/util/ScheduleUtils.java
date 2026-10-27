package com.admin.server.modules.infra.job.util;

import com.admin.server.modules.infra.dal.dataobject.job.SysJobDO;
import org.quartz.*;

import java.util.Date;

public final class ScheduleUtils {

    public static final String TASK_PROPERTIES = "TASK_PROPERTIES";

    private ScheduleUtils() {
    }

    public static JobKey getJobKey(Long jobId, String jobGroup) {
        return JobKey.jobKey("TASK_" + jobId, jobGroup);
    }

    public static TriggerKey getTriggerKey(Long jobId, String jobGroup) {
        return TriggerKey.triggerKey("TASK_" + jobId, jobGroup);
    }

    public static void createScheduleJob(Scheduler scheduler, SysJobDO job) throws SchedulerException {
        Class<? extends Job> jobClass = QuartzJobExecution.class;
        if (job.getConcurrent() != null && job.getConcurrent() == 1) {
            jobClass = QuartzDisallowConcurrentExecution.class;
        }

        JobKey jobKey = getJobKey(job.getId(), job.getJobGroup());
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobKey).build();

        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerKey(job.getId(), job.getJobGroup()))
                .withSchedule(cronScheduleBuilder)
                .build();

        jobDetail.getJobDataMap().put(TASK_PROPERTIES, job);

        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        scheduler.scheduleJob(jobDetail, trigger);

        if (job.getStatus() != null && job.getStatus() == 0) {
            scheduler.pauseJob(jobKey);
        }
    }

    public static void updateScheduleJob(Scheduler scheduler, SysJobDO job) throws SchedulerException {
        JobKey jobKey = getJobKey(job.getId(), job.getJobGroup());
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        createScheduleJob(scheduler, job);
    }

    public static void deleteScheduleJob(Scheduler scheduler, SysJobDO job) throws SchedulerException {
        JobKey jobKey = getJobKey(job.getId(), job.getJobGroup());
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    public static void run(Scheduler scheduler, SysJobDO job) throws SchedulerException {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(TASK_PROPERTIES, job);
        scheduler.triggerJob(getJobKey(job.getId(), job.getJobGroup()), dataMap);
    }

    public static void pauseJob(Scheduler scheduler, SysJobDO job) throws SchedulerException {
        scheduler.pauseJob(getJobKey(job.getId(), job.getJobGroup()));
    }

    public static void resumeJob(Scheduler scheduler, SysJobDO job) throws SchedulerException {
        scheduler.resumeJob(getJobKey(job.getId(), job.getJobGroup()));
    }

    public static Date getNextFireTime(Scheduler scheduler, SysJobDO job) throws SchedulerException {
        TriggerKey triggerKey = getTriggerKey(job.getId(), job.getJobGroup());
        Trigger trigger = scheduler.getTrigger(triggerKey);
        if (trigger != null) {
            return trigger.getNextFireTime();
        }
        return null;
    }

    public static Date getNextFireTime(SysJobDO job) {
        try {
            CronExpression expression = new CronExpression(job.getCronExpression());
            return expression.getNextValidTimeAfter(new Date());
        } catch (Exception e) {
            return null;
        }
    }

    private static CronScheduleBuilder handleCronScheduleMisfirePolicy(SysJobDO job, CronScheduleBuilder cb) {
        if (job.getMisfirePolicy() == null) {
            return cb;
        }
        return switch (job.getMisfirePolicy()) {
            case 1 -> cb.withMisfireHandlingInstructionIgnoreMisfires();
            case 2 -> cb.withMisfireHandlingInstructionFireAndProceed();
            case 3 -> cb.withMisfireHandlingInstructionDoNothing();
            default -> cb;
        };
    }
}
