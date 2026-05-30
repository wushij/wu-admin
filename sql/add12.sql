-- add12.sql — 补全定时任务内置模板（可重复执行）
USE `wu-admin`;
SET NAMES utf8mb4;

INSERT INTO sys_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, remark) VALUES
(4, '字典缓存刷新', 'SYSTEM', 'systemJobTask.refreshDictCache', '0 0 3 * * ?', 3, 1, 0, 'refresh dict redis cache'),
(5, '调度日志清理', 'SYSTEM', 'systemJobTask.purgeExpiredJobLogs', '0 0 4 ? * SUN', 3, 1, 0, 'purge quartz job logs older than 30 days')
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name), invoke_target = VALUES(invoke_target),
    cron_expression = VALUES(cron_expression), remark = VALUES(remark);
