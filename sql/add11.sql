-- =============================================================================
-- add11.sql — 定时任务模块（表结构 + 菜单权限 + 内置任务，可重复执行）
-- 用法：mysql -u root -p wu-admin < sql/add11.sql
-- =============================================================================

USE `wu-admin`;

CREATE TABLE IF NOT EXISTS sys_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    job_name VARCHAR(64) NOT NULL COMMENT '任务名称',
    job_group VARCHAR(64) DEFAULT 'DEFAULT' COMMENT '任务组名',
    invoke_target VARCHAR(500) NOT NULL COMMENT '调用目标字符串',
    cron_expression VARCHAR(255) DEFAULT NULL COMMENT 'cron执行表达式',
    misfire_policy TINYINT DEFAULT 3 COMMENT '计划执行错误策略(1-立即执行 2-执行一次 3-放弃执行)',
    concurrent TINYINT DEFAULT 1 COMMENT '是否并发执行(0-允许 1-禁止)',
    status TINYINT DEFAULT 0 COMMENT '状态(0-暂停 1-正常)',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_job_group (job_group),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表';

CREATE TABLE IF NOT EXISTS sys_job_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    job_name VARCHAR(64) NOT NULL COMMENT '任务名称',
    job_group VARCHAR(64) DEFAULT NULL COMMENT '任务组名',
    invoke_target VARCHAR(500) DEFAULT NULL COMMENT '调用目标字符串',
    job_message VARCHAR(500) DEFAULT NULL COMMENT '日志信息',
    status TINYINT DEFAULT 0 COMMENT '执行状态(0-正常 1-失败)',
    exception_info VARCHAR(2000) DEFAULT NULL COMMENT '异常信息',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    stop_time DATETIME DEFAULT NULL COMMENT '停止时间',
    INDEX idx_job_name (job_name),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务日志表';

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(180, '定时任务', 'monitor:job:list', 2, 3, 100, '/monitor/job', 'Timer', 'monitor/job/index', 1),
(181, '任务查询', 'monitor:job:query', 3, 1, 180, '', '', '', 1),
(182, '任务新增', 'monitor:job:add', 3, 2, 180, '', '', '', 1),
(183, '任务编辑', 'monitor:job:edit', 3, 3, 180, '', '', '', 1),
(184, '任务删除', 'monitor:job:delete', 3, 4, 180, '', '', '', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), permission = VALUES(permission), type = VALUES(type),
    sort = VALUES(sort), parent_id = VALUES(parent_id), path = VALUES(path),
    icon = VALUES(icon), component = VALUES(component), status = VALUES(status);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 180), (1, 181), (1, 182), (1, 183), (1, 184);

INSERT INTO sys_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, remark) VALUES
(1, '过期日志归档清理', 'SYSTEM', 'systemJobTask.purgeExpiredLogs', '0 30 2 * * ?', 3, 1, 0, '清理超保留期的操作/登录/API访问日志'),
(2, '私聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldChatMessages', '0 0 3 * * ?', 3, 1, 0, '清理超过 180 天的私聊记录'),
(3, '群聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldGroupChatMessages', '0 10 3 * * ?', 3, 1, 0, '清理超过 180 天的群聊记录'),
(4, '调度日志清理', 'SYSTEM', 'systemJobTask.purgeExpiredJobLogs', '0 0 4 ? * SUN', 3, 1, 0, '清理 30 天前的 Quartz 调度执行日志'),
(5, '已读通知清理', 'SYSTEM', 'systemJobTask.purgeReadNotices', '0 15 3 * * ?', 3, 1, 0, '清理已读且超过 90 天的站内通知'),
(6, '工单回收站清理', 'SYSTEM', 'systemJobTask.purgeTicketRecycleBin', '0 30 3 * * ?', 3, 1, 0, '彻底删除回收站中超过 30 天的工单')
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name), job_group = VALUES(job_group), invoke_target = VALUES(invoke_target),
    cron_expression = VALUES(cron_expression), remark = VALUES(remark);
