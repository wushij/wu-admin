-- add13.sql — 定时任务模板调整：移除 API 落库/心跳，新增 6 个实用任务
USE `wu-admin`;
SET NAMES utf8mb4;

DELETE FROM sys_job WHERE invoke_target IN (
    'systemJobTask.flushApiAccessLogs',
    'sampleJobTask.heartbeat'
);

INSERT INTO sys_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, remark) VALUES
(1, '过期日志归档清理', 'SYSTEM', 'systemJobTask.purgeExpiredLogs', '0 30 2 * * ?', 3, 1, 0, '清理超保留期的操作/登录/API访问日志'),
(2, '私聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldChatMessages', '0 0 3 * * ?', 3, 1, 0, '清理超过 180 天的私聊记录'),
(3, '群聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldGroupChatMessages', '0 10 3 * * ?', 3, 1, 0, '清理超过 180 天的群聊记录'),
(4, '调度日志清理', 'SYSTEM', 'systemJobTask.purgeExpiredJobLogs', '0 0 4 ? * SUN', 3, 1, 0, '清理 30 天前的 Quartz 调度执行日志'),
(5, '已读通知清理', 'SYSTEM', 'systemJobTask.purgeReadNotices', '0 15 3 * * ?', 3, 1, 0, '清理已读且超过 90 天的站内通知'),
(6, '工单回收站清理', 'SYSTEM', 'systemJobTask.purgeTicketRecycleBin', '0 30 3 * * ?', 3, 1, 0, '彻底删除回收站中超过 30 天的工单')
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    job_group = VALUES(job_group),
    invoke_target = VALUES(invoke_target),
    cron_expression = VALUES(cron_expression),
    remark = VALUES(remark);
