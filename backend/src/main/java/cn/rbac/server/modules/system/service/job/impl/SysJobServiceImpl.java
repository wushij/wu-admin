package cn.rbac.server.modules.system.service.job.impl;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.quartz.util.CronUtils;
import cn.rbac.server.framework.quartz.util.ScheduleUtils;
import cn.rbac.server.modules.system.api.monitor.vo.JobTemplateVO;
import cn.rbac.server.modules.system.api.monitor.vo.SysJobVO;
import cn.rbac.server.modules.system.dal.dataobject.job.SysJobDO;
import cn.rbac.server.modules.system.dal.mysql.job.SysJobMapper;
import cn.rbac.server.modules.system.service.job.SysJobLogService;
import cn.rbac.server.modules.system.service.job.SysJobService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJobDO> implements SysJobService {

    @Resource
    private Scheduler scheduler;
    @Resource
    private SysJobLogService jobLogService;

    @PostConstruct
    public void init() throws SchedulerException {
        scheduler.clear();
        List<SysJobDO> jobList = list();
        for (SysJobDO job : jobList) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>(jobLogService.statistics());
        long totalJobs = count();
        long runningJobs = count(new LambdaQueryWrapper<SysJobDO>().eq(SysJobDO::getStatus, 1));
        result.put("totalJobs", totalJobs);
        result.put("runningJobs", runningJobs);
        result.put("pausedJobs", Math.max(0, totalJobs - runningJobs));
        long totalCount = ((Number) result.getOrDefault("totalCount", 0L)).longValue();
        long successCount = ((Number) result.getOrDefault("successCount", 0L)).longValue();
        result.put("successRate", totalCount == 0 ? 100 : Math.round(successCount * 1000.0 / totalCount) / 10.0);
        return result;
    }

    @Override
    public List<JobTemplateVO> templates() {
        List<JobTemplateVO> list = new ArrayList<>();
        list.add(template("purge_logs", "过期日志归档", "SYSTEM", "清理超保留期的操作/登录/API 访问日志",
                "systemJobTask.purgeExpiredLogs", "0 30 2 * * ?", "系统日志", "Document"));
        list.add(template("purge_chat", "私聊消息清理", "SYSTEM", "清理超过 180 天的私聊记录，防止 sys_chat_message 膨胀",
                "systemJobTask.purgeOldChatMessages", "0 0 3 * * ?", "消息聊天", "ChatDotRound"));
        list.add(template("purge_group_chat", "群聊消息清理", "SYSTEM", "清理超过 180 天的群聊记录，防止 sys_chat_group_message 膨胀",
                "systemJobTask.purgeOldGroupChatMessages", "0 10 3 * * ?", "消息聊天", "ChatLineRound"));
        list.add(template("purge_job_log", "调度日志清理", "SYSTEM", "清理 30 天前的 Quartz 调度执行日志",
                "systemJobTask.purgeExpiredJobLogs", "0 0 4 ? * SUN", "定时任务", "Timer"));
        list.add(template("purge_notices", "已读通知清理", "SYSTEM", "清理已读且超过 90 天的站内通知，减轻消息表压力",
                "systemJobTask.purgeReadNotices", "0 15 3 * * ?", "消息中心", "Bell"));
        list.add(template("purge_ticket_recycle", "工单回收站清理", "SYSTEM", "彻底删除回收站中超过 30 天的工单及关联数据",
                "systemJobTask.purgeTicketRecycleBin", "0 30 3 * * ?", "工单管理", "Tickets"));
        return list;
    }

    private JobTemplateVO template(String key, String name, String group, String desc,
                                   String target, String cron, String module, String icon) {
        JobTemplateVO vo = new JobTemplateVO();
        vo.setKey(key);
        vo.setName(name);
        vo.setJobGroup(group);
        vo.setDescription(desc);
        vo.setInvokeTarget(target);
        vo.setCronExpression(cron);
        vo.setRelatedModule(module);
        vo.setIcon(icon);
        return vo;
    }

    @Override
    public Map<String, Object> checkCron(String cronExpression) {
        CronUtils.validate(cronExpression);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("hint", CronUtils.hint(cronExpression));
        try {
            result.put("nextFireTimes", CronUtils.nextFireTimes(cronExpression, 5));
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析 Cron：" + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public PageResult<SysJobVO> pageVo(Integer pageNo, Integer pageSize, String jobName, String jobGroup, Integer status) {
        PageResult<SysJobDO> page = page(pageNo, pageSize, jobName, jobGroup, status);
        List<SysJobVO> voList = new ArrayList<>();
        for (SysJobDO job : page.getList()) {
            SysJobVO vo = new SysJobVO();
            BeanUtils.copyProperties(job, vo);
            vo.setCronHint(CronUtils.hint(job.getCronExpression()));
            try {
                Date next = ScheduleUtils.getNextFireTime(scheduler, job);
                vo.setNextFireTime(CronUtils.formatDate(next));
            } catch (SchedulerException ignored) {
                vo.setNextFireTime(null);
            }
            voList.add(vo);
        }
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public PageResult<SysJobDO> page(Integer pageNo, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<SysJobDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(jobName), SysJobDO::getJobName, jobName)
                .eq(StringUtils.hasText(jobGroup), SysJobDO::getJobGroup, jobGroup)
                .eq(status != null, SysJobDO::getStatus, status)
                .orderByDesc(SysJobDO::getCreateTime);
        Page<SysJobDO> result = page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public SysJobDO getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysJobDO job) {
        if (!StringUtils.hasText(job.getJobGroup())) {
            job.setJobGroup("DEFAULT");
        }
        if (job.getMisfirePolicy() == null) {
            job.setMisfirePolicy(3);
        }
        if (job.getConcurrent() == null) {
            job.setConcurrent(1);
        }
        if (job.getStatus() == null) {
            job.setStatus(0);
        }
        CronUtils.validate(job.getCronExpression());
        save(job);
        try {
            ScheduleUtils.createScheduleJob(scheduler, job);
        } catch (SchedulerException e) {
            throw new IllegalStateException("创建定时任务失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysJobDO job) {
        SysJobDO existJob = getById(job.getId());
        if (existJob == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        CronUtils.validate(job.getCronExpression());
        updateById(job);
        try {
            ScheduleUtils.updateScheduleJob(scheduler, getById(job.getId()));
        } catch (SchedulerException e) {
            throw new IllegalStateException("更新定时任务失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysJobDO job = getById(id);
        if (job == null) {
            return;
        }
        removeById(id);
        try {
            ScheduleUtils.deleteScheduleJob(scheduler, job);
        } catch (SchedulerException e) {
            throw new IllegalStateException("删除定时任务失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        SysJobDO job = getById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        job.setStatus(status);
        updateById(job);
        try {
            if (status != null && status == 1) {
                ScheduleUtils.resumeJob(scheduler, job);
            } else {
                ScheduleUtils.pauseJob(scheduler, job);
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("修改任务状态失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void run(Long id) {
        SysJobDO job = getById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        try {
            ScheduleUtils.run(scheduler, job);
        } catch (SchedulerException e) {
            throw new IllegalStateException("执行任务失败：" + e.getMessage(), e);
        }
    }
}
