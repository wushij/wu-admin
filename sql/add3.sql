-- =============================================
-- 已有库增量：群聊操作日志（可重复执行，无 DROP）
-- 用法：mysql -u root -p wu-admin < sql/add3.sql
-- 全量新库请使用 admin_platform.sql（已含本表）
-- =============================================

CREATE TABLE IF NOT EXISTS sys_chat_group_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '群ID',
    action_type VARCHAR(32) NOT NULL COMMENT '操作类型',
    operator_id BIGINT NOT NULL COMMENT '操作人',
    operator_name VARCHAR(50) DEFAULT NULL COMMENT '操作人昵称',
    target_user_id BIGINT DEFAULT NULL COMMENT '目标用户',
    target_user_name VARCHAR(50) DEFAULT NULL COMMENT '目标用户昵称',
    detail VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_time (group_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊操作日志';
