-- add14.sql — 字典/配置缓存刷新任务替换为聊天消息清理
USE `wu-admin`;
SET NAMES utf8mb4;

UPDATE sys_job SET
    job_name = '私聊消息清理',
    invoke_target = 'systemJobTask.purgeOldChatMessages',
    cron_expression = '0 0 3 * * ?',
    remark = '清理超过 180 天的私聊记录'
WHERE invoke_target = 'systemJobTask.refreshDictCache' OR id = 2;

UPDATE sys_job SET
    job_name = '群聊消息清理',
    invoke_target = 'systemJobTask.purgeOldGroupChatMessages',
    cron_expression = '0 10 3 * * ?',
    remark = '清理超过 180 天的群聊记录'
WHERE invoke_target = 'systemJobTask.refreshConfigCache' OR id = 3;

INSERT INTO sys_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, remark) VALUES
(2, '私聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldChatMessages', '0 0 3 * * ?', 3, 1, 0, '清理超过 180 天的私聊记录'),
(3, '群聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldGroupChatMessages', '0 10 3 * * ?', 3, 1, 0, '清理超过 180 天的群聊记录')
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    invoke_target = VALUES(invoke_target),
    cron_expression = VALUES(cron_expression),
    remark = VALUES(remark);
