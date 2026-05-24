-- =============================================================================
-- 消息中心增量升级脚本（已有库请执行本文件，不会清空业务数据）
-- 说明：
--   - 使用 CREATE TABLE IF NOT EXISTS / INSERT ... ON DUPLICATE KEY UPDATE
--   - 不含 DROP TABLE，不会删除 sys_notice 等业务数据
--   - 全新空库请执行 sql/admin_platform.sql 全量初始化
-- 执行后：超级管理员 / 普通用户需重新登录以刷新菜单
-- =============================================================================

USE admin_platform;

-- ========== 表结构 ==========
CREATE TABLE IF NOT EXISTS sys_announce (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    notice_type TINYINT DEFAULT 1 COMMENT '1通知 2公告',
    channels VARCHAR(200) DEFAULT '["station"]' COMMENT '推送渠道JSON',
    target_type TINYINT DEFAULT 3 COMMENT '1指定用户 2按部门 3全部',
    target_ids VARCHAR(500) DEFAULT NULL COMMENT '目标ID JSON数组',
    status TINYINT DEFAULT 0 COMMENT '0草稿 1已发布',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_name VARCHAR(50) DEFAULT NULL COMMENT '创建人昵称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator VARCHAR(64) DEFAULT '',
    updater VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    INDEX idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知/公告';

CREATE TABLE IF NOT EXISTS sys_user_announce (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收用户',
    announce_id BIGINT NOT NULL COMMENT '通知ID',
    is_read TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    read_time DATETIME DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_announce (user_id, announce_id),
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知已读';

CREATE TABLE IF NOT EXISTS sys_announce_send_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    announce_id BIGINT NOT NULL,
    channel VARCHAR(50) NOT NULL COMMENT 'station等',
    status TINYINT DEFAULT 1 COMMENT '0失败 1成功',
    target_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    error_msg VARCHAR(500) DEFAULT NULL,
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_announce_id (announce_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知推送日志';

CREATE TABLE IF NOT EXISTS sys_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(50) DEFAULT NULL,
    sender_avatar VARCHAR(255) DEFAULT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT,
    msg_type TINYINT DEFAULT 1 COMMENT '1文本 2图片',
    is_read TINYINT DEFAULT 0,
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_pair_time (sender_id, receiver_id, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息';

CREATE TABLE IF NOT EXISTS sys_user_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '拉黑方',
    blocked_user_id BIGINT NOT NULL COMMENT '被拉黑用户',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_blocked (user_id, blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天黑名单';

CREATE TABLE IF NOT EXISTS sys_chat_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500) DEFAULT NULL,
    owner_id BIGINT NOT NULL,
    announcement VARCHAR(500) DEFAULT NULL,
    max_members INT DEFAULT 200,
    status TINYINT DEFAULT 1 COMMENT '0解散 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊';

CREATE TABLE IF NOT EXISTS sys_chat_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    role TINYINT DEFAULT 0 COMMENT '0成员 1管理员 2群主',
    muted TINYINT DEFAULT 0,
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员';

CREATE TABLE IF NOT EXISTS sys_chat_group_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(50) DEFAULT NULL,
    sender_avatar VARCHAR(500) DEFAULT NULL,
    content TEXT NOT NULL,
    msg_type TINYINT DEFAULT 1,
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_time (group_id, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群消息';

-- ========== 菜单与权限（170-178）==========
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(170, '消息中心', '', 1, 7, 0, '/message', 'Bell', '', 1),
(171, '系统通知', 'system:announce:list', 2, 1, 170, '/message/notice', 'Notification', 'message/notice/index', 1),
(172, '即时聊天', 'system:chat:list', 2, 2, 170, '/message/chat', 'ChatDotRound', 'message/chat/index', 1),
(173, '通知查询', 'system:announce:query', 3, 1, 171, '', '', '', 1),
(174, '通知新增', 'system:announce:create', 3, 2, 171, '', '', '', 1),
(175, '通知修改', 'system:announce:update', 3, 3, 171, '', '', '', 1),
(176, '通知删除', 'system:announce:delete', 3, 4, 171, '', '', '', 1),
(177, '通知发布', 'system:announce:publish', 3, 5, 171, '', '', '', 1),
(178, '聊天查询', 'system:chat:query', 3, 1, 172, '', '', '', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    path = VALUES(path),
    icon = VALUES(icon),
    component = VALUES(component),
    status = VALUES(status);

-- 仅追加消息中心菜单权限，不影响其它角色菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 170), (1, 171), (1, 172), (1, 173), (1, 174), (1, 175), (1, 176), (1, 177), (1, 178),
(2, 170), (2, 172), (2, 178);
