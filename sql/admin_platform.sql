-- =============================================================================
-- Admin Platform 数据库脚本（唯一入口）
-- 数据库: wu-admin  |  字符集: utf8mb4_unicode_ci
-- =============================================================================
--
-- 【使用方式】
--   全新安装（空库）  直接执行全文即可（自动检测空库放行 Part A/B）
--                     示例: mysql -u root -p < sql/admin_platform.sql
--   已有库（有表）    勿跑全文 Part A/B；仅执行文末「附录」
--   强制重装         SET @WU_ADMIN_ALLOW_DROP=1; 后再执行全文（会 DROP 清库）
--
-- 【正文结构】
--   Part A  建表      §1 用户 ~ §18 AI wu助手（gen_table 含 uk_gen_table_name_deleted 唯一索引）
--   Part B  初始数据  组织/用户/字典/配置/菜单（含代码生成 164-169,179、AI 管理 200-206,210）/定时任务/角色权限
--
-- 【附录】旧库补丁（含代码生成表/菜单/唯一索引迁移及历次发版变更）
-- =============================================================================

-- 建库并切换
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

-- Part A/B 熔断：空库自动放行；已有表则拦截（防误删）；@WU_ADMIN_ALLOW_DROP=1 可强制重装
SET @WU_ADMIN_ALLOW_DROP := IFNULL(@WU_ADMIN_ALLOW_DROP, 0);
SET @WU_ADMIN_TABLE_CNT := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'wu-admin'
      AND table_type = 'BASE TABLE'
);

DROP PROCEDURE IF EXISTS sp_wu_admin_require_drop;
DELIMITER $$
CREATE PROCEDURE sp_wu_admin_require_drop()
BEGIN
    IF @WU_ADMIN_ALLOW_DROP = 1 OR @WU_ADMIN_TABLE_CNT = 0 THEN
        SELECT IF(@WU_ADMIN_TABLE_CNT = 0, '[OK] empty database, fresh install',
                  '[OK] @WU_ADMIN_ALLOW_DROP=1, forced reinstall') AS result;
    ELSE
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Part A/B 已拦截：wu-admin 已有表。旧库请执行文末附录；重装请 SET @WU_ADMIN_ALLOW_DROP=1;';
    END IF;
END$$
DELIMITER ;
CALL sp_wu_admin_require_drop();
DROP PROCEDURE IF EXISTS sp_wu_admin_require_drop;

-- =============================================================================
-- Part A  建表（DROP + CREATE）
-- =============================================================================

-- §1 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    gender TINYINT DEFAULT 0 COMMENT '性别(0-未知 1-男 2-女)',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用 2:待审核 3:审核未通过',
    dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_username (username),
    INDEX idx_mobile (mobile),
    INDEX idx_dept_id (dept_id),
    INDEX idx_deleted_status (deleted, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- §2 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL COMMENT '角色编码',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    data_scope TINYINT DEFAULT 1 COMMENT '数据范围 1:全部 2:自定义 3:本部门 4:本部门及以下 5:仅本人',
    data_scope_dept_ids VARCHAR(500) DEFAULT NULL COMMENT '数据范围部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- §3 菜单表
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    permission VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    type TINYINT NOT NULL COMMENT '类型 1:目录 2:菜单 3:按钮',
    sort INT DEFAULT 0 COMMENT '排序',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    path VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    icon VARCHAR(100) DEFAULT NULL COMMENT '图标',
    component VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- §4 部门表
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    ancestors VARCHAR(500) DEFAULT '' COMMENT '祖级列表，如 0,1,5',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    leader_name VARCHAR(50) DEFAULT NULL COMMENT '负责人',
    leader_user_id BIGINT DEFAULT NULL COMMENT '负责人用户ID',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- §4.1 岗位表
DROP TABLE IF EXISTS sys_user_post;
DROP TABLE IF EXISTS sys_post;
CREATE TABLE sys_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '岗位ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父岗位ID',
    post_code VARCHAR(50) NOT NULL COMMENT '岗位编码',
    post_name VARCHAR(50) NOT NULL COMMENT '岗位名称',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_post_code (post_code),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

CREATE TABLE sys_user_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    post_id BIGINT NOT NULL COMMENT '岗位ID',
    INDEX idx_user_id (user_id),
    INDEX idx_post_id (post_id),
    UNIQUE KEY uk_user_post (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户岗位关联表';

-- §5 用户角色关联
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- §6 角色菜单关联
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- §7 登录日志
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    ipaddr VARCHAR(50) COMMENT 'IP地址',
    login_location VARCHAR(100) COMMENT '登录地点',
    browser VARCHAR(50) COMMENT '浏览器',
    os VARCHAR(50) COMMENT '操作系统',
    status TINYINT DEFAULT 0 COMMENT '登录状态(0-成功 1-失败)',
    msg VARCHAR(255) COMMENT '消息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- §7.1 字典类型 / 字典数据
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    status TINYINT DEFAULT 1 COMMENT '状态(0停用 1正常)',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted INT DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

CREATE TABLE sys_dict_data (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    sort INT DEFAULT 0 COMMENT '排序',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典键值',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    css_class VARCHAR(100) DEFAULT NULL COMMENT '样式属性',
    list_class VARCHAR(100) DEFAULT NULL COMMENT '回显样式',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认(0否 1是)',
    status TINYINT DEFAULT 1 COMMENT '状态(0停用 1正常)',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted INT DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_value (dict_type, dict_value),  -- 不含 deleted；软删后同 value 再建需应用层处理
    KEY idx_dict_type (dict_type),
    KEY idx_dict_type_status_deleted (dict_type, status, deleted, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- §7.2 操作日志
DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(50) DEFAULT NULL COMMENT '模块标题',
    business_type INT DEFAULT 0 COMMENT '业务类型(0其它 1新增 2修改 3删除 4查询 5导出 6导入)',
    method VARCHAR(100) DEFAULT NULL COMMENT '方法名称',
    request_method VARCHAR(10) DEFAULT NULL COMMENT '请求方式',
    oper_name VARCHAR(50) DEFAULT NULL COMMENT '操作人员',
    oper_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    oper_ip VARCHAR(128) DEFAULT NULL COMMENT '主机地址',
    oper_param VARCHAR(2000) DEFAULT NULL COMMENT '请求参数',
    json_result VARCHAR(2000) DEFAULT NULL COMMENT '返回参数',
    status INT DEFAULT 0 COMMENT '操作状态(0正常 1异常)',
    error_msg VARCHAR(2000) DEFAULT NULL COMMENT '错误消息',
    oper_time DATETIME DEFAULT NULL COMMENT '操作时间',
    cost_time BIGINT DEFAULT 0 COMMENT '消耗时间(ms)',
    PRIMARY KEY (id),
    INDEX idx_oper_time (oper_time),
    INDEX idx_oper_name (oper_name),
    INDEX idx_title (title),
    INDEX idx_oper_time_status (oper_time, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- §7.3 定时任务（sys_job / sys_job_log，Quartz 调度）
DROP TABLE IF EXISTS sys_job_log;
DROP TABLE IF EXISTS sys_job;
CREATE TABLE sys_job (
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

CREATE TABLE sys_job_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    job_name VARCHAR(64) NOT NULL COMMENT '任务名称',
    job_group VARCHAR(64) DEFAULT NULL COMMENT '任务组名',
    invoke_target VARCHAR(500) DEFAULT NULL COMMENT '调用目标字符串',
    job_message VARCHAR(500) DEFAULT NULL COMMENT '日志信息',
    status TINYINT DEFAULT 0 COMMENT '执行状态(0-正常 1-失败)',
    exception_info VARCHAR(2000) DEFAULT NULL COMMENT '异常信息',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    stop_time DATETIME DEFAULT NULL COMMENT '停止时间',
    duration_ms BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_name (job_name),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务日志表';

-- §8 工单
DROP TABLE IF EXISTS sys_ticket;
CREATE TABLE sys_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_no VARCHAR(64) NOT NULL COMMENT '工单编号',
    title VARCHAR(200) NOT NULL COMMENT '工单标题',
    description TEXT COMMENT '工单描述',
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级 LOW/MEDIUM/HIGH/URGENT',
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT '状态 OPEN/IN_PROGRESS/RESOLVED/CLOSED',
    creator_user_id BIGINT NOT NULL COMMENT '创建人ID',
    assignee_user_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    deadline DATETIME DEFAULT NULL COMMENT '截止时间',
    closed_time DATETIME DEFAULT NULL COMMENT '关闭时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_ticket_no (ticket_no),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_creator_user_id (creator_user_id),
    INDEX idx_assignee_user_id (assignee_user_id),
    INDEX idx_deleted_status_time (deleted, status, create_time),
    INDEX idx_deleted_update_time (deleted, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- §9 工单评论
DROP TABLE IF EXISTS sys_ticket_comment;
CREATE TABLE sys_ticket_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单评论表';

-- §10 工单附件
DROP TABLE IF EXISTS sys_ticket_attachment;
CREATE TABLE sys_ticket_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    uploader_user_id BIGINT NOT NULL COMMENT '上传用户ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_uploader_user_id (uploader_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单附件表';

-- §11 站内消息（业务收件箱 sys_notice，工单/审批触达）
DROP TABLE IF EXISTS sys_notice;
CREATE TABLE sys_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content VARCHAR(1000) NOT NULL COMMENT '消息内容',
    biz_type VARCHAR(32) DEFAULT NULL COMMENT '业务类型',
    biz_id BIGINT DEFAULT NULL COMMENT '业务ID',
    read_status TINYINT DEFAULT 0 COMMENT '读取状态 0-未读 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user_read_deleted (user_id, read_status, deleted),
    INDEX idx_read_create_time (read_status, create_time),
    INDEX idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- §11b 消息中心（广播通知 sys_announce + 企业IM 私聊/群聊）
--      sys_notice 保留为业务收件箱；sys_announce 为管理员发布的广播通知
DROP TABLE IF EXISTS sys_chat_group_log;
DROP TABLE IF EXISTS sys_chat_group_message;
DROP TABLE IF EXISTS sys_chat_group_member;
DROP TABLE IF EXISTS sys_chat_group;
DROP TABLE IF EXISTS sys_user_blacklist;
DROP TABLE IF EXISTS sys_chat_message;
DROP TABLE IF EXISTS sys_announce_send_log;
DROP TABLE IF EXISTS sys_user_announce;
DROP TABLE IF EXISTS sys_announce;

CREATE TABLE sys_announce (
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

CREATE TABLE sys_user_announce (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收用户',
    announce_id BIGINT NOT NULL COMMENT '通知ID',
    is_read TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    read_time DATETIME DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_announce (user_id, announce_id),
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知已读';

CREATE TABLE sys_announce_send_log (
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

CREATE TABLE sys_chat_message (
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
    INDEX idx_receiver_unread (receiver_id, is_read, sender_id),
    INDEX idx_pair_time (sender_id, receiver_id, send_time),
    INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息';

CREATE TABLE sys_user_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '拉黑方',
    blocked_user_id BIGINT NOT NULL COMMENT '被拉黑用户',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_blocked (user_id, blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天黑名单';

CREATE TABLE sys_chat_group (
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

CREATE TABLE sys_chat_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    role TINYINT DEFAULT 0 COMMENT '0成员 1管理员 2群主',
    muted TINYINT DEFAULT 0,
    notify_muted TINYINT DEFAULT 0 COMMENT '0正常 1免打扰仅@提醒',
    announcement_read_time DATETIME DEFAULT NULL COMMENT '群公告已读时间',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员';

CREATE TABLE sys_chat_group_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(50) DEFAULT NULL,
    sender_avatar VARCHAR(500) DEFAULT NULL,
    content TEXT NOT NULL,
    msg_type TINYINT DEFAULT 1,
    mention_ids VARCHAR(500) DEFAULT NULL COMMENT '@的用户ID列表JSON',
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_time (group_id, send_time),
    INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群消息';

CREATE TABLE sys_chat_group_log (
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

-- §12 审批单
DROP TABLE IF EXISTS sys_approval_form;
CREATE TABLE sys_approval_form (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    form_no VARCHAR(64) NOT NULL COMMENT '审批单号',
    form_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '类型',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    status VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态',
    applicant_user_id BIGINT NOT NULL COMMENT '申请人ID',
    approver_user_id BIGINT NOT NULL COMMENT '审批人ID',
    result_remark VARCHAR(1000) DEFAULT NULL COMMENT '审批结果备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_form_no (form_no),
    INDEX idx_status (status),
    INDEX idx_applicant_user_id (applicant_user_id),
    INDEX idx_approver_user_id (approver_user_id),
    INDEX idx_type_applicant_status (form_type, applicant_user_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批单表';

-- §13 审批记录
DROP TABLE IF EXISTS sys_approval_record;
CREATE TABLE sys_approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    form_id BIGINT NOT NULL COMMENT '审批单ID',
    operator_user_id BIGINT NOT NULL COMMENT '操作人ID',
    action VARCHAR(16) NOT NULL COMMENT '操作动作',
    remark VARCHAR(1000) DEFAULT NULL COMMENT '操作备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_form_id (form_id),
    INDEX idx_operator_user_id (operator_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- §14 API 访问统计日志
DROP TABLE IF EXISTS sys_api_access_log;
CREATE TABLE sys_api_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    start_time DATETIME NULL DEFAULT NULL COMMENT '请求开始时间',
    end_time DATETIME NULL DEFAULT NULL COMMENT '请求结束时间',
    api_path VARCHAR(500) NULL DEFAULT NULL COMMENT 'API路径',
    method VARCHAR(10) NULL DEFAULT NULL COMMENT 'HTTP方法',
    status_code INT NULL DEFAULT NULL COMMENT 'HTTP状态码',
    success TINYINT NULL DEFAULT 1 COMMENT '是否成功(0否 1是)',
    cost_time BIGINT NULL DEFAULT NULL COMMENT '耗时(毫秒)',
    ip VARCHAR(64) NULL DEFAULT NULL COMMENT '客户端IP',
    user_id BIGINT NULL DEFAULT NULL COMMENT '用户ID(未登录为空)',
    PRIMARY KEY (id),
    INDEX idx_start_time (start_time),
    INDEX idx_start_time_success (start_time, success),
    INDEX idx_api_path (api_path(100)),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API访问统计日志';

-- §15 文件管理（sys_file / sys_file_group）
DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    original_name VARCHAR(200) DEFAULT '' COMMENT '原始文件名',
    file_name VARCHAR(200) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    url VARCHAR(500) DEFAULT '' COMMENT '访问URL',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    file_type VARCHAR(100) DEFAULT '' COMMENT 'MIME',
    file_suffix VARCHAR(20) DEFAULT '' COMMENT '后缀',
    storage_type VARCHAR(20) DEFAULT 'local' COMMENT '存储类型',
    bucket_name VARCHAR(100) DEFAULT '' COMMENT '桶名',
    group_id BIGINT DEFAULT NULL COMMENT '分组ID',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id),
    INDEX idx_group_id (group_id),
    INDEX idx_create_time (create_time),
    INDEX idx_group_time (group_id, create_time),
    INDEX idx_deleted_update (deleted, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

DROP TABLE IF EXISTS sys_file_group;
CREATE TABLE sys_file_group (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    name VARCHAR(100) NOT NULL COMMENT '分组名称',
    sort INT DEFAULT 0 COMMENT '排序',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分组表';

-- §15b 短信发送记录（sys_sms_log）
DROP TABLE IF EXISTS sys_sms_log;
CREATE TABLE sys_sms_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    content VARCHAR(500) DEFAULT NULL COMMENT '短信内容/验证码',
    sms_type VARCHAR(20) DEFAULT 'verify_code' COMMENT 'verify_code / notice / marketing',
    template_id VARCHAR(50) DEFAULT NULL COMMENT '模板ID',
    template_params VARCHAR(500) DEFAULT NULL COMMENT '模板参数 JSON',
    provider VARCHAR(20) DEFAULT NULL COMMENT 'aliyun / tencent / console',
    status TINYINT DEFAULT 0 COMMENT '0-发送中 1-成功 2-失败',
    result_msg VARCHAR(500) DEFAULT NULL COMMENT '结果信息',
    biz_id VARCHAR(100) DEFAULT NULL COMMENT '服务商消息ID',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    biz_type VARCHAR(30) DEFAULT NULL COMMENT '业务类型',
    ip VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_phone_create_time (phone, create_time),
    INDEX idx_create_time (create_time),
    INDEX idx_send_time (send_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信发送记录表';

-- §16 系统配置分组（sys_config_group，JSON 按 group_code 存储）
DROP TABLE IF EXISTS sys_config_group;
CREATE TABLE sys_config_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    group_code VARCHAR(50) NOT NULL COMMENT '分组编码 login/register',
    group_name VARCHAR(100) NOT NULL COMMENT '分组名称',
    config_value TEXT NOT NULL COMMENT 'JSON 配置',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator VARCHAR(64) DEFAULT '',
    updater VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_group_code (group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置分组';

-- §17 代码生成（gen_table / gen_table_column）
DROP TABLE IF EXISTS gen_table_column;
DROP TABLE IF EXISTS gen_table;
CREATE TABLE gen_table (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    table_name VARCHAR(200) DEFAULT '' COMMENT '表名称',
    table_comment VARCHAR(500) DEFAULT '' COMMENT '表描述',
    class_name VARCHAR(100) DEFAULT '' COMMENT '实体类名称',
    package_name VARCHAR(100) DEFAULT '' COMMENT '生成包路径',
    module_name VARCHAR(30) DEFAULT '' COMMENT '生成模块名',
    business_name VARCHAR(30) DEFAULT '' COMMENT '生成业务名',
    function_name VARCHAR(50) DEFAULT '' COMMENT '生成功能名',
    author VARCHAR(50) DEFAULT '' COMMENT '作者',
    gen_type VARCHAR(10) DEFAULT 'crud' COMMENT '生成类型',
    gen_path VARCHAR(200) DEFAULT '/' COMMENT '生成路径',
    front_type VARCHAR(30) DEFAULT 'element-plus' COMMENT '前端模板',
    form_layout VARCHAR(20) DEFAULT 'vertical' COMMENT '表单布局',
    parent_menu_id BIGINT DEFAULT NULL COMMENT '上级菜单ID',
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_gen_table_name_deleted (table_name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表';

CREATE TABLE gen_table_column (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_id BIGINT DEFAULT NULL,
    column_name VARCHAR(200) DEFAULT '',
    column_comment VARCHAR(500) DEFAULT '',
    column_type VARCHAR(100) DEFAULT '',
    java_type VARCHAR(50) DEFAULT '',
    java_field VARCHAR(200) DEFAULT '',
    is_pk TINYINT DEFAULT 0,
    is_increment TINYINT DEFAULT 0,
    is_required TINYINT DEFAULT 0,
    is_insert TINYINT DEFAULT 0,
    is_edit TINYINT DEFAULT 0,
    is_list TINYINT DEFAULT 0,
    is_query TINYINT DEFAULT 0,
    query_type VARCHAR(20) DEFAULT 'EQ',
    html_type VARCHAR(50) DEFAULT '',
    dict_type VARCHAR(100) DEFAULT '',
    sort INT DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_gen_col_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成字段表';

-- §18 AI wu助手（sys_ai_model / sys_ai_chat_log）
DROP TABLE IF EXISTS sys_ai_chat_log;
DROP TABLE IF EXISTS sys_ai_model;
CREATE TABLE sys_ai_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模型配置ID',
    name VARCHAR(100) NOT NULL COMMENT '配置名称（展示用，如「DeepSeek 官方」）',
    provider VARCHAR(32) NOT NULL COMMENT '供应商: deepseek/openai/qwen/kimi',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称（如 deepseek-chat / qwen-plus）',
    base_url VARCHAR(255) NOT NULL COMMENT 'API 基础地址（不含 /chat/completions）',
    api_key VARCHAR(1024) DEFAULT '' COMMENT 'API 密钥（SM4-CBC 加密存储）',
    temperature DECIMAL(3,2) DEFAULT 0.70 COMMENT '采样温度 0~2',
    max_tokens INT DEFAULT 4096 COMMENT '单次回复最大 Token 数',
    system_prompt VARCHAR(2000) DEFAULT NULL COMMENT '系统提示词（角色设定）',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认模型 0:否 1:是（全局唯一）',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_provider (provider),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型供应商配置表';

CREATE TABLE sys_ai_chat_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT DEFAULT 0 COMMENT '提问用户ID',
    username VARCHAR(64) DEFAULT '' COMMENT '提问用户名',
    conversation_id VARCHAR(64) DEFAULT '' COMMENT '会话ID（前端生成，串联多轮对话）',
    model_id BIGINT DEFAULT 0 COMMENT '模型配置ID',
    provider VARCHAR(32) DEFAULT '' COMMENT '供应商',
    model_name VARCHAR(100) DEFAULT '' COMMENT '模型名称',
    question TEXT COMMENT '用户提问（脱敏后）',
    answer MEDIUMTEXT COMMENT 'AI 回答',
    prompt_tokens INT DEFAULT 0 COMMENT '提问 Token 消耗',
    completion_tokens INT DEFAULT 0 COMMENT '回答 Token 消耗',
    total_tokens INT DEFAULT 0 COMMENT '总 Token 消耗',
    duration_ms BIGINT DEFAULT 0 COMMENT '耗时（毫秒）',
    chat_status TINYINT DEFAULT 1 COMMENT '结果 1:成功 0:失败 2:用户中断',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    source VARCHAR(20) DEFAULT 'pc' COMMENT '来源终端 pc/mobile',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user_id (user_id),
    INDEX idx_model_id (model_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话日志审计表';

-- =============================================================================
-- Part B  初始化数据
-- =============================================================================

-- 部门（id=1 为本部虚拟根，界面隐藏；二级为各中心）
INSERT INTO sys_dept (id, name, parent_id, ancestors, sort, status, leader_name, leader_user_id) VALUES
(1, '本部', 0, '0', 0, 1, '管理员', 1),
(2, '技术中心', 1, '0,1', 1, 1, NULL, NULL),
(3, '业务中心', 1, '0,1', 2, 1, NULL, NULL),
(4, '职能中心', 1, '0,1', 3, 1, NULL, NULL),
(5, '运营中心', 1, '0,1', 4, 1, NULL, NULL),
(6, '研发部', 2, '0,1,2', 1, 1, '张三', 2),
(7, '运维部', 2, '0,1,2', 2, 1, NULL),
(8, '产品部', 2, '0,1,2', 3, 1, NULL),
(9, '市场部', 3, '0,1,3', 1, 1, '李四'),
(10, '财务部', 4, '0,1,4', 1, 1, '王五'),
(11, '人事部', 4, '0,1,4', 2, 1, NULL),
(12, '实训部', 5, '0,1,5', 1, 1, NULL),
(13, '客服部', 5, '0,1,5', 2, 1, NULL),
(14, '前端组', 6, '0,1,2,6', 1, 1, NULL),
(15, '后端组', 6, '0,1,2,6', 2, 1, NULL);

-- 岗位（树形层级）
INSERT INTO sys_post (id, parent_id, post_code, post_name, sort, status, remark) VALUES
(1, 0, 'chairman', '董事长', 0, 1, '岗位体系根'),
(2, 1, 'ceo', '总经理', 1, 1, ''),
(3, 2, 'cto', '技术总监', 1, 1, ''),
(4, 2, 'dev_exec', '开发工程师', 2, 1, ''),
(5, 2, 'biz_line', '业务体系', 3, 1, '岗位分类'),
(6, 2, 'func_line', '职能体系', 4, 1, '岗位分类'),
(7, 2, 'ops_line', '运营体系', 5, 1, '岗位分类'),
(8, 3, 'dev', '研发工程师', 1, 1, ''),
(9, 8, 'dev_lead', '研发组长', 0, 1, ''),
(10, 8, 'fe_dev', '前端开发', 1, 1, ''),
(11, 8, 'be_dev', '后端开发', 2, 1, ''),
(12, 3, 'qa', '测试', 2, 1, ''),
(13, 12, 'qa_lead', '测试组长', 0, 1, ''),
(14, 3, 'product_mgr', '产品经理', 3, 1, ''),
(15, 3, 'ops_eng', '运维工程师', 4, 1, ''),
(16, 5, 'market_spec', '市场专员', 1, 1, ''),
(17, 6, 'finance_mgr', '财务主管', 1, 1, ''),
(18, 6, 'hr_spec', '人事专员', 2, 1, ''),
(19, 7, 'train_lecturer', '实训讲师', 1, 1, '');

ALTER TABLE sys_dept AUTO_INCREMENT = 16;
ALTER TABLE sys_post AUTO_INCREMENT = 20;

-- 用户（默认密码 admin123，BCrypt 加密）
INSERT INTO sys_user (id, username, password, nickname, mobile, email, status, dept_id) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', '13800138000', 'admin@admin.cn', 1, 1),
(2, 'zhangsan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', '13800138001', 'zhangsan@admin.cn', 1, 6);

-- 角色
INSERT INTO sys_role (id, name, code, sort, status, remark) VALUES
(1, '超级管理员', 'super_admin', 1, 1, '超级管理员，拥有所有权限'),
(2, '普通用户', 'user', 2, 1, '仅部分功能');

-- 字典（含工单/审批业务字典 type 4～7）
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(1, '系统状态', 'sys_normal_disable', 1, '通用启用停用'),
(2, '用户性别', 'sys_user_sex', 1, '用户性别'),
(3, '是否', 'sys_yes_no', 1, '是或否'),
(4, '工单状态', 'sys_ticket_status', 1, '工单流转状态'),
(5, '工单优先级', 'sys_ticket_priority', 1, '工单优先级'),
(6, '审批类型', 'sys_approval_form_type', 1, '审批单业务类型'),
(7, '审批状态', 'sys_approval_status', 1, '审批单流转状态');

INSERT INTO sys_dict_data (dict_type, sort, dict_label, dict_value, list_class, is_default, status) VALUES
('sys_normal_disable', 1, '启用', '1', 'success', 1, 1),
('sys_normal_disable', 2, '禁用', '0', 'danger', 0, 1),
('sys_user_sex', 1, '男', '1', 'primary', 0, 1),
('sys_user_sex', 2, '女', '2', 'danger', 0, 1),
('sys_user_sex', 3, '未知', '0', 'info', 1, 1),
('sys_yes_no', 1, '是', 'Y', 'success', 1, 1),
('sys_yes_no', 2, '否', 'N', 'info', 0, 1),
('sys_ticket_status', 1, '待处理', 'OPEN', 'info', 1, 1),
('sys_ticket_status', 2, '处理中', 'IN_PROGRESS', 'warning', 0, 1),
('sys_ticket_status', 3, '已解决', 'RESOLVED', 'success', 0, 1),
('sys_ticket_status', 4, '已关闭', 'CLOSED', 'danger', 0, 1),
('sys_ticket_priority', 1, '低', 'LOW', 'info', 0, 1),
('sys_ticket_priority', 2, '中', 'MEDIUM', 'success', 1, 1),
('sys_ticket_priority', 3, '高', 'HIGH', 'warning', 0, 1),
('sys_ticket_priority', 4, '紧急', 'URGENT', 'danger', 0, 1),
('sys_approval_form_type', 1, '通用', 'GENERAL', 'info', 1, 1),
('sys_approval_form_type', 2, '请假', 'LEAVE', 'primary', 0, 1),
('sys_approval_form_type', 3, '采购', 'PURCHASE', 'warning', 0, 1),
('sys_approval_form_type', 4, '报销', 'REIMBURSE', 'success', 0, 1),
('sys_approval_form_type', 5, '用印', 'SEAL', 'danger', 0, 1),
('sys_approval_form_type', 6, '合同', 'CONTRACT', 'info', 0, 1),
('sys_approval_form_type', 7, '注册审核', 'REGISTER', 'warning', 0, 1),
('sys_approval_status', 1, '待审批', 'SUBMITTED', 'warning', 1, 1),
('sys_approval_status', 2, '已通过', 'APPROVED', 'success', 0, 1),
('sys_approval_status', 3, '已驳回', 'REJECTED', 'danger', 0, 1),
('sys_approval_status', 4, '已归档', 'ARCHIVED', 'info', 0, 1);

-- 系统配置（11 分组：site / session / file / rateLimit / login / register / thirdParty / payment / sms / email / security）
INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('site', '基础信息', '{"platformName":"Admin Platform","platformSubtitle":"统一运维 · 高效管控","loginWelcome":"Welcome","registerTitle":"Sign Up","copyright":""}', '平台展示名称与登录页文案'),
('session', '会话配置', '{"tokenExpireHours":24}', 'JWT 与 Redis 会话有效期（小时）'),
('file', '文件配置', '{"maxSizeMb":50,"allowedExtensions":"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov"}', '文件管理上传限制'),
('rateLimit', '接口限流', '{"captchaPerIpMinute":40,"loginPerIpMinute":30,"registerPerIpMinute":10,"smsPerIpMinute":5,"smsSendIntervalSeconds":60,"smsPerPhoneDaily":10,"smsPerIpDaily":30}', '认证接口按 IP 限流；含短信防刷'),
('login', '登录配置', '{"captchaEnabled":true,"captchaType":"image","smsLoginEnabled":false,"smsLoginSliderCaptchaEnabled":false,"rememberMe":true,"maxRetryCount":5,"maxRetryCountIp":20,"lockTime":10}', '验证码 image/slider；smsLoginEnabled 短信登录；smsLoginSliderCaptchaEnabled 短信发送前滑块；maxRetryCount 账号锁定阈值；maxRetryCountIp IP 锁定阈值'),
('register', '注册配置', '{"enabled":true,"captchaEnabled":true,"captchaType":"image","defaultRoleCode":"user","needAudit":false,"minPasswordLength":6}', '开放注册、验证码类型、默认角色、是否审核'),
('thirdParty', '第三方配置', '{"wechat":{"enabled":false,"appId":"","appSecret":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":""},"github":{"enabled":false,"clientId":"","clientSecret":""},"google":{"enabled":false,"clientId":"","clientSecret":"","redirectUri":""}}', '微信/支付宝/GitHub/Google 第三方登录'),
('payment', '支付配置', '{"wechatPay":{"enabled":false,"mchId":"","appId":"","apiV3Key":"","privateKey":"","certSerialNo":"","notifyUrl":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":"","signType":"RSA2","gatewayUrl":"https://openapi.alipay.com/gateway.do","notifyUrl":"","returnUrl":""}}', '微信/支付宝支付与测试下单'),
('sms', '短信配置', '{"enabled":false,"provider":"aliyunAuth","accessKeyId":"","accessKeySecret":"","signName":"","tencentAppId":"","templateVerifyCode":"100001","templateModifyPhone":"100002","templateResetPassword":"100003","templateBindPhone":"100004","templateVerifyBindPhone":"100005","schemeName":"","codeExpireMinutes":5}', '阿里云短信认证/腾讯云'),
('email', '邮件配置', '{"enabled":true,"provider":"qq","host":"smtp.qq.com","port":465,"username":"974473458@qq.com","password":"cqjvfpulydqwbegh","fromName":"wu-admin 系统团队","authEnabled":true,"securityType":"SSL","connectionTimeoutMs":5000,"timeoutMs":5000,"writeTimeoutMs":5000,"encoding":"UTF-8","debug":false,"codeExpireMinutes":5,"codeLength":6,"dailyLimitPerEmail":20,"sendIntervalSeconds":60}', '企业级 SMTP 邮件服务配置：发件人、SSL端口、超时限额与验证码防刷规则'),
('security', '安全配置', '{"disableDevtool":false,"isConcurrent":false,"sm4EncryptEnabled":true,"sm2SignEnabled":true,"sm3SignEnabled":true,"timestampEnabled":true,"nonceEnabled":true}', '安全防线与会话：SM4加密（会话密钥）、HMAC-SM3数字签名、时间戳与Nonce防重放');

-- 菜单与按钮（一级目录 sort：系统管理 1 / 监控 3 / 日志 4 / 文件 5 / 消息 6 / 流程 7 / 工具 8）
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
-- 系统管理目录
(1, '系统管理', '', 1, 1, 0, '/system', 'Setting', '', 1),
-- 用户管理
(2, '用户管理', 'system:user:list', 2, 1, 1, '/system/user', 'User', 'system/user/index', 1),
-- 角色管理
(3, '角色管理', 'system:role:list', 2, 2, 1, '/system/role', 'UserFilled', 'system/role/index', 1),
-- 菜单管理
(4, '菜单管理', 'system:menu:list', 2, 3, 1, '/system/menu', 'Menu', 'system/menu/index', 1),
-- 组织管理（部门 + 岗位）
(5, '组织管理', 'system:dept:list', 2, 4, 1, '/system/org', 'OfficeBuilding', 'system/org/index', 1),
-- 字典管理
(130, '字典管理', 'system:dict:list', 2, 5, 1, '/system/dict', 'Collection', 'system/dict/index', 1),
(160, '系统配置', 'system:config:list', 2, 6, 1, '/system/config', 'Tools', 'system/config/index', 1),
(163, '回收中心', 'system:recycle:list', 2, 7, 1, '/system/recycle', 'Delete', 'system/recycle/index', 1),
(191, '回收中心查询', 'system:recycle:query', 3, 1, 163, '', '', '', 1),
(192, '回收中心恢复', 'system:recycle:restore', 3, 2, 163, '', '', '', 1),
(193, '回收中心删除', 'system:recycle:delete', 3, 3, 163, '', '', '', 1),
-- 流程中心目录（审批 + 工单）
(8, '流程中心', '', 1, 7, 0, '/workflow', 'Operation', '', 1),
-- 审批单中心
(9, '审批单中心', 'system:approval:list', 2, 1, 8, '/system/approval', 'Checked', 'system/approval/index', 1),
-- 工单管理（隶属流程中心）
(7, '工单管理', 'system:ticket:list', 2, 2, 8, '/system/ticket', 'Tickets', 'system/ticket/index', 1),
-- 用户管理按钮
(10, '用户查询', 'system:user:query', 3, 1, 2, '', '', '', 1),
(11, '用户新增', 'system:user:create', 3, 2, 2, '', '', '', 1),
(12, '用户修改', 'system:user:update', 3, 3, 2, '', '', '', 1),
(13, '用户删除', 'system:user:delete', 3, 4, 2, '', '', '', 1),
-- 角色管理按钮
(20, '角色查询', 'system:role:query', 3, 1, 3, '', '', '', 1),
(21, '角色新增', 'system:role:create', 3, 2, 3, '', '', '', 1),
(22, '角色修改', 'system:role:update', 3, 3, 3, '', '', '', 1),
(23, '角色删除', 'system:role:delete', 3, 4, 3, '', '', '', 1),
-- 菜单管理按钮
(30, '菜单查询', 'system:menu:query', 3, 1, 4, '', '', '', 1),
(31, '菜单新增', 'system:menu:create', 3, 2, 4, '', '', '', 1),
(32, '菜单修改', 'system:menu:update', 3, 3, 4, '', '', '', 1),
(33, '菜单删除', 'system:menu:delete', 3, 4, 4, '', '', '', 1),
-- 部门管理按钮
(40, '部门查询', 'system:dept:query', 3, 1, 5, '', '', '', 1),
(41, '部门新增', 'system:dept:create', 3, 2, 5, '', '', '', 1),
(42, '部门修改', 'system:dept:update', 3, 3, 5, '', '', '', 1),
(43, '部门删除', 'system:dept:delete', 3, 4, 5, '', '', '', 1),
-- 岗位管理按钮（组织管理页内）
(44, '岗位查询', 'system:post:query', 3, 5, 5, '', '', '', 1),
(45, '岗位新增', 'system:post:create', 3, 6, 5, '', '', '', 1),
(46, '岗位修改', 'system:post:update', 3, 7, 5, '', '', '', 1),
(47, '岗位删除', 'system:post:delete', 3, 8, 5, '', '', '', 1),
-- 字典管理按钮
(131, '字典查询', 'system:dict:query', 3, 1, 130, '', '', '', 1),
(132, '字典新增', 'system:dict:create', 3, 2, 130, '', '', '', 1),
(133, '字典修改', 'system:dict:update', 3, 3, 130, '', '', '', 1),
(134, '字典删除', 'system:dict:delete', 3, 4, 130, '', '', '', 1),
(135, '字典复制', 'system:dict:copy', 3, 5, 130, '', '', '', 1),
(161, '配置查询', 'system:config:query', 3, 1, 160, '', '', '', 1),
(162, '配置修改', 'system:config:update', 3, 2, 160, '', '', '', 1),
-- 登录日志按钮
(50, '日志查询', 'system:loginLog:query', 3, 1, 6, '', '', '', 1),
(51, '日志删除', 'system:loginLog:delete', 3, 2, 6, '', '', '', 1),
(52, '日志清空', 'system:loginLog:clear', 3, 3, 6, '', '', '', 1),
-- 工单按钮
(60, '工单查询', 'system:ticket:query', 3, 1, 7, '', '', '', 1),
(61, '工单创建', 'system:ticket:create', 3, 2, 7, '', '', '', 1),
(62, '工单更新', 'system:ticket:update', 3, 3, 7, '', '', '', 1),
(63, '工单流转', 'system:ticket:transition', 3, 4, 7, '', '', '', 1),
(64, '工单评论', 'system:ticket:comment', 3, 5, 7, '', '', '', 1),
(65, '工单删除', 'system:ticket:delete', 3, 6, 7, '', '', '', 1),
-- 审批单按钮
(70, '审批单查询', 'system:approval:query', 3, 1, 9, '', '', '', 1),
(71, '审批单创建', 'system:approval:create', 3, 2, 9, '', '', '', 1),
(72, '审批单审批', 'system:approval:approve', 3, 3, 9, '', '', '', 1),
(73, '审批单归档', 'system:approval:archive', 3, 4, 9, '', '', '', 1),
(74, '审批单删除', 'system:approval:delete', 3, 5, 9, '', '', '', 1),
-- 文件管理目录（与系统管理、系统监控同级）
(105, '文件管理', '', 1, 5, 0, '/file', 'Folder', '', 1),
(110, '文件列表', 'sys:file:list', 2, 1, 105, '/system/file', 'DocumentCopy', 'system/file/index', 1),
(111, '文件查询', 'sys:file:query', 3, 1, 110, '', '', '', 1),
(112, '文件上传', 'sys:file:upload', 3, 2, 110, '', '', '', 1),
(113, '文件删除', 'sys:file:delete', 3, 3, 110, '', '', '', 1),
-- 系统监控目录
(100, '系统监控', '', 1, 3, 0, '/monitor', 'Monitor', '', 1),
-- API 访问统计
(101, 'API访问统计', 'monitor:apiAccess:list', 2, 6, 100, '/monitor/api-access', 'DataLine', 'monitor/api-access/index', 1),
(102, '访问统计查询', 'monitor:apiAccess:query', 3, 1, 101, '', '', '', 1),
-- 在线用户
(103, '在线用户', 'monitor:online:list', 2, 2, 100, '/monitor/online', 'User', 'monitor/online/index', 1),
(190, '在线用户查询', 'monitor:online:query', 3, 1, 103, '', '', '', 1),
(104, '在线用户强退', 'monitor:online:forceLogout', 3, 2, 103, '', '', '', 1),
-- 定时任务
(180, '定时任务', 'monitor:job:list', 2, 3, 100, '/monitor/job', 'Timer', 'monitor/job/index', 1),
(181, '任务查询', 'monitor:job:query', 3, 1, 180, '', '', '', 1),
(182, '任务新增', 'monitor:job:add', 3, 2, 180, '', '', '', 1),
(183, '任务编辑', 'monitor:job:edit', 3, 3, 180, '', '', '', 1),
(184, '任务删除', 'monitor:job:delete', 3, 4, 180, '', '', '', 1),
-- 缓存监控
(185, '缓存监控', 'monitor:cache:list', 2, 4, 100, '/monitor/cache', 'Coin', 'monitor/cache/index', 1),
(188, '缓存查询', 'monitor:cache:query', 3, 1, 185, '', '', '', 1),
(186, '缓存删除', 'monitor:cache:delete', 3, 2, 185, '', '', '', 1),
-- 服务监控
(187, '服务监控', 'monitor:server:list', 2, 5, 100, '/monitor/server', 'Cpu', 'monitor/server/index', 1),
(189, '服务监控查询', 'monitor:server:query', 3, 1, 187, '', '', '', 1),
-- 系统日志目录
(120, '系统日志', '', 1, 4, 0, '/log', 'Notebook', '', 1),
(121, '操作日志', 'system:operLog:list', 2, 1, 120, '/system/oper-log', 'EditPen', 'system/oper-log/index', 1),
(126, '操作日志查询', 'system:operLog:query', 3, 1, 121, '', '', '', 1),
(127, '操作日志删除', 'system:operLog:delete', 3, 2, 121, '', '', '', 1),
(128, '操作日志清空', 'system:operLog:clear', 3, 3, 121, '', '', '', 1),
-- 登录日志（隶属系统日志）
(6, '登录日志', 'system:loginLog:list', 2, 2, 120, '/system/login-log', 'Promotion', 'system/login-log/index', 1),
-- 开发工具
(150, '开发工具', '', 1, 9, 0, '/tool', 'Tools', '', 1),
(151, '接口文档', 'tool:apiDoc:view', 2, 1, 150, '/tool/api-doc', 'Connection', '/doc.html', 1),
(164, '代码生成', 'tool:gen:list', 2, 2, 150, '/tool/gen', 'SetUp', 'tool/gen/index', 1),
(165, '代码生成查询', 'tool:gen:query', 3, 1, 164, '', '', '', 1),
(166, '代码生成导入', 'tool:gen:import', 3, 2, 164, '', '', '', 1),
(167, '代码生成修改', 'tool:gen:edit', 3, 3, 164, '', '', '', 1),
(168, '代码生成删除', 'tool:gen:remove', 3, 4, 164, '', '', '', 1),
(169, '代码生成预览', 'tool:gen:preview', 3, 5, 164, '', '', '', 1),
(179, '代码生成执行', 'tool:gen:code', 3, 6, 164, '', '', '', 1),
-- 消息中心
(170, '消息中心', '', 1, 6, 0, '/message', 'Bell', '', 1),
(171, '系统通知', 'system:announce:list', 2, 1, 170, '/message/notice', 'Notification', 'message/notice/index', 1),
(172, '企业IM', 'system:chat:list', 2, 2, 170, '/message/chat', 'ChatDotRound', 'message/chat/index', 1),
(173, '通知查询', 'system:announce:query', 3, 1, 171, '', '', '', 1),
(174, '通知新增', 'system:announce:create', 3, 2, 171, '', '', '', 1),
(175, '通知修改', 'system:announce:update', 3, 3, 171, '', '', '', 1),
(176, '通知删除', 'system:announce:delete', 3, 4, 171, '', '', '', 1),
(177, '通知发布', 'system:announce:publish', 3, 5, 171, '', '', '', 1),
(178, '聊天查询', 'system:chat:query', 3, 1, 172, '', '', '', 1),
-- AI 管理（顶级目录 210，排在开发工具之前）
(210, 'AI 管理', '', 1, 8, 0, '/ai', 'MagicStick', '', 1),
(200, 'AI 模型配置', 'system:ai-model:list', 2, 1, 210, '/ai/model', 'MagicStick', 'ai/model/index', 1),
(201, 'AI模型新增', 'system:ai-model:create', 3, 1, 200, '', '', '', 1),
(202, 'AI模型修改', 'system:ai-model:update', 3, 2, 200, '', '', '', 1),
(203, 'AI模型删除', 'system:ai-model:delete', 3, 3, 200, '', '', '', 1),
(204, 'AI模型测试', 'system:ai-model:test', 3, 4, 200, '', '', '', 1),
(205, 'AI 对话日志', 'system:ai-log:list', 2, 2, 210, '/ai/log', 'ChatDotRound', 'ai/log/index', 1),
(206, 'AI日志删除', 'system:ai-log:delete', 3, 1, 205, '', '', '', 1);

-- 内置定时任务（默认暂停 status=0，在「系统监控 → 定时任务」启用）
INSERT INTO sys_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, remark) VALUES
(1, '过期日志归档清理', 'SYSTEM', 'systemJobTask.purgeExpiredLogs', '0 30 2 * * ?', 3, 1, 0, '清理超保留期的操作/登录/API访问日志'),
(2, '私聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldChatMessages', '0 0 3 * * ?', 3, 1, 0, '清理超过 180 天的私聊记录'),
(3, '群聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldGroupChatMessages', '0 10 3 * * ?', 3, 1, 0, '清理超过 180 天的群聊记录'),
(4, '调度日志清理', 'SYSTEM', 'systemJobTask.purgeExpiredJobLogs', '0 0 4 ? * SUN', 3, 1, 0, '清理 30 天前的 Quartz 调度执行日志'),
(5, '已读通知清理', 'SYSTEM', 'systemJobTask.purgeReadNotices', '0 15 3 * * ?', 3, 1, 0, '清理已读且超过 90 天的站内通知'),
(6, '工单回收站清理', 'SYSTEM', 'systemJobTask.purgeTicketRecycleBin', '0 30 3 * * ?', 3, 1, 0, '彻底删除回收站中超过 30 天的工单');

-- 用户 ↔ 角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin 拥有超级管理员角色
(2, 2); -- zhangsan 拥有普通用户角色

-- 超级管理员 ↔ 全部菜单
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 130), (1, 160), (1, 163), (1, 191), (1, 192), (1, 193), (1, 7), (1, 8), (1, 9),
(1, 6), (1, 120), (1, 121), (1, 126), (1, 127), (1, 128),
(1, 10), (1, 11), (1, 12), (1, 13),
(1, 20), (1, 21), (1, 22), (1, 23),
(1, 30), (1, 31), (1, 32), (1, 33),
(1, 40), (1, 41), (1, 42), (1, 43), (1, 44), (1, 45), (1, 46), (1, 47),
(1, 131), (1, 132), (1, 133), (1, 134), (1, 135), (1, 161), (1, 162),
(1, 50), (1, 51), (1, 52),
(1, 60), (1, 61), (1, 62), (1, 63), (1, 64), (1, 65),
(1, 70), (1, 71), (1, 72), (1, 73), (1, 74),
(1, 100), (1, 101), (1, 102), (1, 103), (1, 190), (1, 104), (1, 180), (1, 181), (1, 182), (1, 183), (1, 184), (1, 185), (1, 186), (1, 187), (1, 188), (1, 189),
(1, 105), (1, 110), (1, 111), (1, 112), (1, 113),
(1, 150), (1, 151), (1, 164), (1, 165), (1, 166), (1, 167), (1, 168), (1, 169), (1, 179),
(1, 170), (1, 171), (1, 172), (1, 173), (1, 174), (1, 175), (1, 176), (1, 177), (1, 178),
(1, 210), (1, 200), (1, 201), (1, 202), (1, 203), (1, 204), (1, 205), (1, 206);

-- 普通用户默认权限（页面+查询按钮；侧栏父级由 getUserMenuList 自动补齐）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 2), (2, 10), (2, 3), (2, 20), (2, 4), (2, 30), (2, 5), (2, 40),
(2, 130), (2, 131), (2, 160), (2, 161),
(2, 8), (2, 7), (2, 60), (2, 61), (2, 62), (2, 63), (2, 64),
(2, 9), (2, 70), (2, 71), (2, 73),
(2, 101), (2, 102),
(2, 105), (2, 110), (2, 111), (2, 112),
(2, 121), (2, 126), (2, 6), (2, 50),
(2, 151),
(2, 170), (2, 172), (2, 178);

-- =============================================================================
-- ▼▼▼ MySQL 5.6 空库安装请在此停止 ▼▼▼
-- 5.6 无法执行下方附录（JSON_SET / JSON_EXTRACT 等需 MySQL 5.7.8+）
-- 空库请改用: mysql -u wuadmin -p wuadmin < sql/admin_platform_mysql56.sql（服务器 · 库名 wuadmin）
-- =============================================================================

-- =============================================================================
-- 附录：已有库升级（极旧库首次补丁；可重复执行，无 DROP）
-- 执行: 在客户端选中本节至文件末尾，或 mysql ... wu-admin < admin_platform.sql 仅当已跳过 Part A/B
-- 菜单默认 INSERT IGNORE，不覆盖 name/path/icon
-- =============================================================================

SET @WU_ADMIN_SYNC_MENU := IFNULL(@WU_ADMIN_SYNC_MENU, 0);

-- [附录·基础] 极旧库可能无配置分组表
CREATE TABLE IF NOT EXISTS sys_config_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    group_code VARCHAR(50) NOT NULL COMMENT '分组编码',
    group_name VARCHAR(100) NOT NULL COMMENT '分组名称',
    config_value TEXT NOT NULL COMMENT 'JSON 配置',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator VARCHAR(64) DEFAULT '',
    updater VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_group_code (group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置分组';

-- [附录·配置] 注册/登录 captchaType 补全（缺省 image）
UPDATE sys_config_group
SET config_value = JSON_SET(
        COALESCE(config_value, '{}'),
        '$.captchaType',
        COALESCE(
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')), ''),
                'image'
        )
    ),
    remark = '开放注册、验证码类型、默认角色、是否审核'
WHERE group_code = 'register'
  AND (
    JSON_EXTRACT(config_value, '$.captchaType') IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')) = ''
    );

UPDATE sys_config_group
SET config_value = JSON_SET(
        COALESCE(config_value, '{}'),
        '$.captchaType',
        COALESCE(
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')), ''),
                'image'
        )
    )
WHERE group_code = 'login'
  AND (
    JSON_EXTRACT(config_value, '$.captchaType') IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')) = ''
    );

-- [附录·配置] 第三方 + 支付分组（ON DUPLICATE 不覆盖已有 config_value）
INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('thirdParty', '第三方配置',
 '{"wechat":{"enabled":false,"appId":"","appSecret":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":""},"github":{"enabled":false,"clientId":"","clientSecret":""},"google":{"enabled":false,"clientId":"","clientSecret":"","redirectUri":""}}',
 '微信/支付宝/GitHub/Google 第三方登录密钥'),
('payment', '支付配置',
 '{"wechatPay":{"enabled":false,"mchId":"","appId":"","apiV3Key":"","privateKey":"","certSerialNo":"","notifyUrl":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":"","signType":"RSA2","gatewayUrl":"https://openapi.alipay.com/gateway.do","notifyUrl":"","returnUrl":""}}',
 '微信/支付宝支付与测试下单')
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    remark = VALUES(remark);

-- [附录·配置] thirdParty 补 Google 登录字段
UPDATE sys_config_group
SET config_value = JSON_SET(
        config_value,
        '$.google',
        JSON_OBJECT('enabled', false, 'clientId', '', 'clientSecret', '', 'redirectUri', '')
    )
WHERE group_code = 'thirdParty'
  AND (JSON_EXTRACT(config_value, '$.google') IS NULL);

-- [附录·菜单] 系统配置页 160-162（INSERT IGNORE；强制同步见 sp_wu_admin_sync_builtin_menus）
INSERT IGNORE INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(160, '系统配置', 'system:config:list', 2, 6, 1, '/system/config', 'Tools', 'system/config/index', 1),
(163, '回收中心', 'system:recycle:list', 2, 7, 1, '/system/recycle', 'Delete', 'system/recycle/index', 1),
(191, '回收中心查询', 'system:recycle:query', 3, 1, 163, '', '', '', 1),
(192, '回收中心恢复', 'system:recycle:restore', 3, 2, 163, '', '', '', 1),
(193, '回收中心删除', 'system:recycle:delete', 3, 3, 163, '', '', '', 1),
(161, '配置查询', 'system:config:query', 3, 1, 160, '', '', '', 1),
(162, '配置修改', 'system:config:update', 3, 2, 160, '', '', '', 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 160), (1, 161), (1, 162), (1, 163), (1, 191), (1, 192), (1, 193);

-- [附录·菜单] 代码生成 164-169,179（开发工具下；179 避开消息中心 170）
INSERT IGNORE INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(164, '代码生成', 'tool:gen:list', 2, 2, 150, '/tool/gen', 'SetUp', 'tool/gen/index', 1),
(165, '代码生成查询', 'tool:gen:query', 3, 1, 164, '', '', '', 1),
(166, '代码生成导入', 'tool:gen:import', 3, 2, 164, '', '', '', 1),
(167, '代码生成修改', 'tool:gen:edit', 3, 3, 164, '', '', '', 1),
(168, '代码生成删除', 'tool:gen:remove', 3, 4, 164, '', '', '', 1),
(169, '代码生成预览', 'tool:gen:preview', 3, 5, 164, '', '', '', 1),
(179, '代码生成执行', 'tool:gen:code', 3, 6, 164, '', '', '', 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 164), (1, 165), (1, 166), (1, 167), (1, 168), (1, 169), (1, 179);

-- [附录·表] 代码生成元数据表
CREATE TABLE IF NOT EXISTS gen_table (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    table_name VARCHAR(200) DEFAULT '' COMMENT '表名称',
    table_comment VARCHAR(500) DEFAULT '' COMMENT '表描述',
    class_name VARCHAR(100) DEFAULT '' COMMENT '实体类名称',
    package_name VARCHAR(100) DEFAULT '' COMMENT '生成包路径',
    module_name VARCHAR(30) DEFAULT '' COMMENT '生成模块名',
    business_name VARCHAR(30) DEFAULT '' COMMENT '生成业务名',
    function_name VARCHAR(50) DEFAULT '' COMMENT '生成功能名',
    author VARCHAR(50) DEFAULT '' COMMENT '作者',
    gen_type VARCHAR(10) DEFAULT 'crud' COMMENT '生成类型',
    gen_path VARCHAR(200) DEFAULT '/' COMMENT '生成路径',
    front_type VARCHAR(30) DEFAULT 'element-plus' COMMENT '前端模板',
    form_layout VARCHAR(20) DEFAULT 'vertical' COMMENT '表单布局',
    parent_menu_id BIGINT DEFAULT NULL COMMENT '上级菜单ID',
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_gen_table_name_deleted (table_name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表';

CREATE TABLE IF NOT EXISTS gen_table_column (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_id BIGINT DEFAULT NULL,
    column_name VARCHAR(200) DEFAULT '',
    column_comment VARCHAR(500) DEFAULT '',
    column_type VARCHAR(100) DEFAULT '',
    java_type VARCHAR(50) DEFAULT '',
    java_field VARCHAR(200) DEFAULT '',
    is_pk TINYINT DEFAULT 0,
    is_increment TINYINT DEFAULT 0,
    is_required TINYINT DEFAULT 0,
    is_insert TINYINT DEFAULT 0,
    is_edit TINYINT DEFAULT 0,
    is_list TINYINT DEFAULT 0,
    is_query TINYINT DEFAULT 0,
    query_type VARCHAR(20) DEFAULT 'EQ',
    html_type VARCHAR(50) DEFAULT '',
    dict_type VARCHAR(100) DEFAULT '',
    sort INT DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_gen_col_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成字段表';

-- [附录·表] gen_table 唯一索引升级（#12；IF NOT EXISTS 无法修改已存在表的索引）
DELETE c FROM gen_table_column c
INNER JOIN gen_table t ON c.table_id = t.id
INNER JOIN gen_table dup ON dup.table_name = t.table_name AND dup.id < t.id;

DELETE t1 FROM gen_table t1
INNER JOIN gen_table t2 ON t1.table_name = t2.table_name AND t1.id > t2.id;

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;
DROP PROCEDURE IF EXISTS sp_add_unique_index_if_not_exists;

DELIMITER $$

CREATE PROCEDURE sp_drop_index_if_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;

    IF v_cnt > 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[DROP] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

CREATE PROCEDURE sp_add_unique_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;

    IF v_cnt = 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE INDEX `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[ADD] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

DELIMITER ;

CALL sp_drop_index_if_exists('gen_table', 'idx_gen_table_name');
CALL sp_drop_index_if_exists('gen_table', 'uk_gen_table_name');
CALL sp_add_unique_index_if_not_exists('gen_table', 'uk_gen_table_name', 'table_name');

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;
DROP PROCEDURE IF EXISTS sp_add_unique_index_if_not_exists;

-- [附录·表] gen_table 软删除 + 回收中心（#13）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gen_table'
      AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE gen_table ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''是否删除'' AFTER update_time',
    'SELECT ''deleted exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE gen_table SET deleted = 0 WHERE deleted IS NULL;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists$$
CREATE PROCEDURE sp_drop_index_if_exists(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;
    IF v_cnt > 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS sp_add_unique_index_if_not_exists$$
CREATE PROCEDURE sp_add_unique_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;
    IF v_cnt = 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE INDEX `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL sp_drop_index_if_exists('gen_table', 'uk_gen_table_name');
CALL sp_add_unique_index_if_not_exists('gen_table', 'uk_gen_table_name_deleted', 'table_name, deleted');

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;
DROP PROCEDURE IF EXISTS sp_add_unique_index_if_not_exists;

-- [附录·菜单] 图标修正
UPDATE sys_menu SET icon = 'UserFilled' WHERE id = 3 AND icon IN ('Key', 'key');
UPDATE sys_menu SET icon = 'DocumentCopy' WHERE id = 110;
UPDATE sys_menu SET icon = 'Tickets' WHERE id = 7;
UPDATE sys_menu SET icon = 'Connection' WHERE id = 151;
UPDATE sys_menu SET icon = 'SetUp' WHERE id = 164;

-- [附录·菜单] 操作日志「查询」按钮 126
INSERT IGNORE INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(126, '操作日志查询', 'system:operLog:query', 3, 1, 121, '', '', '', 1);

UPDATE sys_menu
SET permission = 'system:operLog:query',
    name = '操作日志查询',
    type = 3,
    sort = 1,
    parent_id = 121,
    status = 1
WHERE id = 126
  AND (permission IS NULL OR permission = '' OR permission <> 'system:operLog:query');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 126);

-- [附录·消息] 表与菜单补建（正文 §11b 已全量建表）
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
    INDEX idx_receiver_unread (receiver_id, is_read, sender_id),
    INDEX idx_pair_time (sender_id, receiver_id, send_time),
    INDEX idx_send_time (send_time)
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
    notify_muted TINYINT DEFAULT 0 COMMENT '0正常 1免打扰仅@提醒',
    announcement_read_time DATETIME DEFAULT NULL COMMENT '群公告已读时间',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员';

CREATE TABLE IF NOT EXISTS sys_chat_group_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(50) DEFAULT NULL,
    sender_avatar VARCHAR(500) DEFAULT NULL,
    content TEXT NOT NULL,
    msg_type TINYINT DEFAULT 1,
    mention_ids VARCHAR(500) DEFAULT NULL COMMENT '@的用户ID列表JSON',
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_time (group_id, send_time),
    INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群消息';

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

INSERT IGNORE INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(170, '消息中心', '', 1, 6, 0, '/message', 'Bell', '', 1),
(171, '系统通知', 'system:announce:list', 2, 1, 170, '/message/notice', 'Notification', 'message/notice/index', 1),
(172, '企业IM', 'system:chat:list', 2, 2, 170, '/message/chat', 'ChatDotRound', 'message/chat/index', 1),
(173, '通知查询', 'system:announce:query', 3, 1, 171, '', '', '', 1),
(174, '通知新增', 'system:announce:create', 3, 2, 171, '', '', '', 1),
(175, '通知修改', 'system:announce:update', 3, 3, 171, '', '', '', 1),
(176, '通知删除', 'system:announce:delete', 3, 4, 171, '', '', '', 1),
(177, '通知发布', 'system:announce:publish', 3, 5, 171, '', '', '', 1),
(178, '聊天查询', 'system:chat:query', 3, 1, 172, '', '', '', 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 170), (1, 171), (1, 172), (1, 173), (1, 174), (1, 175), (1, 176), (1, 177), (1, 178),
(2, 170), (2, 172), (2, 178);

-- [附录·修复] 操作日志 oper_name 误存 userId → 回填 username
UPDATE sys_oper_log o
INNER JOIN sys_user u ON u.id = CAST(o.oper_name AS UNSIGNED) AND u.deleted = 0
SET o.oper_name = u.username
WHERE o.oper_name REGEXP '^[0-9]+$';

-- [附录·权限] 普通用户：仅补缺失默认菜单（不 DELETE，保留 role_id=2 已自定义权限）
UPDATE sys_role SET remark = '仅部分功能'
WHERE id = 2 AND (remark IS NULL OR remark = '' OR remark = '普通用户');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2, 2), (2, 10), (2, 3), (2, 20), (2, 4), (2, 30), (2, 5), (2, 40),
(2, 130), (2, 131), (2, 160), (2, 161),
(2, 8), (2, 7), (2, 60), (2, 61), (2, 62), (2, 63), (2, 64),
(2, 9), (2, 70), (2, 71), (2, 73),
(2, 101), (2, 102),
(2, 105), (2, 110), (2, 111), (2, 112),
(2, 121), (2, 126), (2, 6), (2, 50),
(2, 151),
(2, 170), (2, 172), (2, 178);

-- [附录·修复] 待审核用户（status=2）补建 REGISTER 审批单
SET @register_approver_id := (
    SELECT ur.user_id
    FROM sys_user_role ur
    INNER JOIN sys_role r ON r.id = ur.role_id AND r.code = 'super_admin' AND r.deleted = 0
    ORDER BY ur.user_id
    LIMIT 1
);

INSERT INTO sys_approval_form (form_no, form_type, title, content, status, applicant_user_id, approver_user_id, creator, updater)
SELECT
    CONCAT('RG', UNIX_TIMESTAMP(), LPAD(u.id, 4, '0')),
    'REGISTER',
    CONCAT('用户注册审核 - ', u.username),
    JSON_OBJECT(
        'bizType', 'USER_REGISTER',
        'userId', u.id,
        'username', u.username,
        'nickname', IFNULL(u.nickname, ''),
        'mobile', IFNULL(u.mobile, '')
    ),
    'SUBMITTED',
    u.id,
    @register_approver_id,
    'system',
    'system'
FROM sys_user u
WHERE u.deleted = 0
  AND u.status = 2
  AND @register_approver_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_approval_form f
    WHERE f.deleted = 0
      AND f.form_type = 'REGISTER'
      AND f.applicant_user_id = u.id
      AND f.status = 'SUBMITTED'
  );

-- [附录·字典] 通用状态文案统一（启用/禁用）
UPDATE sys_dict_data SET dict_label = '启用' WHERE dict_type = 'sys_normal_disable' AND dict_value = '1';
UPDATE sys_dict_data SET dict_label = '禁用' WHERE dict_type = 'sys_normal_disable' AND dict_value = '0';

-- [附录·组织] 扁平部门树 → 中心分级（正文 Part B 已是分级结果，旧库才需执行）
UPDATE sys_dept SET name = '本部' WHERE id = 1 AND name = '总公司' AND deleted = 0;

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '技术中心', 1, '0,1', 1, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '技术中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '业务中心', 1, '0,1', 2, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '业务中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '职能中心', 1, '0,1', 3, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '职能中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '运营中心', 1, '0,1', 4, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '运营中心' AND parent_id = 1 AND deleted = 0);

UPDATE sys_dept d
INNER JOIN sys_dept c ON c.name = '技术中心' AND c.parent_id = 1 AND c.deleted = 0
SET d.parent_id = c.id, d.ancestors = CONCAT(c.ancestors, ',', c.id)
WHERE d.name IN ('研发部', '运维部', '产品部') AND d.deleted = 0 AND d.parent_id = 1;

UPDATE sys_dept d
INNER JOIN sys_dept c ON c.name = '业务中心' AND c.parent_id = 1 AND c.deleted = 0
SET d.parent_id = c.id, d.ancestors = CONCAT(c.ancestors, ',', c.id)
WHERE d.name = '市场部' AND d.deleted = 0 AND d.parent_id = 1;

UPDATE sys_dept d
INNER JOIN sys_dept c ON c.name = '职能中心' AND c.parent_id = 1 AND c.deleted = 0
SET d.parent_id = c.id, d.ancestors = CONCAT(c.ancestors, ',', c.id)
WHERE d.name IN ('财务部', '人事部') AND d.deleted = 0 AND d.parent_id = 1;

UPDATE sys_dept d
INNER JOIN sys_dept c ON c.name = '运营中心' AND c.parent_id = 1 AND c.deleted = 0
SET d.parent_id = c.id, d.ancestors = CONCAT(c.ancestors, ',', c.id)
WHERE d.name IN ('实训部', '客服部') AND d.deleted = 0 AND d.parent_id = 1;

UPDATE sys_dept g
INNER JOIN sys_dept rd ON rd.name = '研发部' AND rd.deleted = 0
SET g.parent_id = rd.id, g.ancestors = CONCAT(rd.ancestors, ',', rd.id)
WHERE g.name IN ('前端组', '后端组') AND g.deleted = 0
  AND (g.parent_id <> rd.id OR g.ancestors NOT LIKE CONCAT(rd.ancestors, ',', rd.id, '%'));

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'chairman', '董事长', 0, 1, '岗位体系根' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'chairman' AND deleted = 0);

UPDATE sys_post ceo
INNER JOIN sys_post ch ON ch.post_code = 'chairman' AND ch.deleted = 0
SET ceo.parent_id = ch.id, ceo.sort = 1
WHERE ceo.post_code = 'ceo' AND ceo.deleted = 0 AND ceo.parent_id = 0;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'cto', '技术总监', 1, 1, ''
FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'cto' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'dev_exec', '开发工程师', 2, 1, ''
FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'dev_exec' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'biz_line', '业务体系', 3, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'biz_line' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'func_line', '职能体系', 4, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'func_line' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'ops_line', '运营体系', 5, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'ops_line' AND deleted = 0)
LIMIT 1;

UPDATE sys_post x
INNER JOIN sys_post cto ON cto.post_code = 'cto' AND cto.deleted = 0
SET x.parent_id = cto.id
WHERE x.post_code IN ('dev', 'qa', 'product_mgr', 'ops_eng') AND x.deleted = 0 AND x.parent_id = 0;

UPDATE sys_post x
INNER JOIN sys_post bl ON bl.post_code = 'biz_line' AND bl.deleted = 0
SET x.parent_id = bl.id
WHERE x.post_code = 'market_spec' AND x.deleted = 0 AND x.parent_id = 0;

UPDATE sys_post x
INNER JOIN sys_post fl ON fl.post_code = 'func_line' AND fl.deleted = 0
SET x.parent_id = fl.id
WHERE x.post_code IN ('finance_mgr', 'hr_spec') AND x.deleted = 0 AND x.parent_id = 0;

UPDATE sys_post x
INNER JOIN sys_post ol ON ol.post_code = 'ops_line' AND ol.deleted = 0
SET x.parent_id = ol.id
WHERE x.post_code = 'train_lecturer' AND x.deleted = 0 AND x.parent_id = 0;

UPDATE sys_post x
INNER JOIN sys_post dev ON dev.post_code = 'dev' AND dev.deleted = 0
SET x.parent_id = dev.id
WHERE x.post_code IN ('dev_lead', 'fe_dev', 'be_dev') AND x.deleted = 0 AND x.parent_id <> dev.id;

UPDATE sys_post x
INNER JOIN sys_post qa ON qa.post_code = 'qa' AND qa.deleted = 0
SET x.parent_id = qa.id
WHERE x.post_code = 'qa_lead' AND x.deleted = 0 AND x.parent_id <> qa.id;

-- [附录·菜单] 一级目录排序（日志 4 / 文件 5 / 消息 6 / 工具 7）
UPDATE sys_menu SET sort = 4 WHERE id = 120 AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE id = 105 AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 6 WHERE id = 170 AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 7 WHERE id = 150 AND parent_id = 0 AND deleted = 0;

-- [附录·菜单] 可选强制同步内置菜单（默认跳过；慎用: SET @WU_ADMIN_SYNC_MENU=1）
DROP PROCEDURE IF EXISTS sp_wu_admin_sync_builtin_menus;
DELIMITER $$
CREATE PROCEDURE sp_wu_admin_sync_builtin_menus()
BEGIN
    IF IFNULL(@WU_ADMIN_SYNC_MENU, 0) <> 1 THEN
        SELECT '[SKIP] builtin menu sync (@WU_ADMIN_SYNC_MENU=0)' AS result;
    ELSE
        INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
        (160, '系统配置', 'system:config:list', 2, 6, 1, '/system/config', 'Tools', 'system/config/index', 1),
        (163, '回收中心', 'system:recycle:list', 2, 7, 1, '/system/recycle', 'Delete', 'system/recycle/index', 1),
        (161, '配置查询', 'system:config:query', 3, 1, 160, '', '', '', 1),
        (162, '配置修改', 'system:config:update', 3, 2, 160, '', '', '', 1),
        (126, '操作日志查询', 'system:operLog:query', 3, 1, 121, '', '', '', 1),
        (170, '消息中心', '', 1, 6, 0, '/message', 'Bell', '', 1),
        (171, '系统通知', 'system:announce:list', 2, 1, 170, '/message/notice', 'Notification', 'message/notice/index', 1),
        (172, '企业IM', 'system:chat:list', 2, 2, 170, '/message/chat', 'ChatDotRound', 'message/chat/index', 1),
        (173, '通知查询', 'system:announce:query', 3, 1, 171, '', '', '', 1),
        (174, '通知新增', 'system:announce:create', 3, 2, 171, '', '', '', 1),
        (175, '通知修改', 'system:announce:update', 3, 3, 171, '', '', '', 1),
        (176, '通知删除', 'system:announce:delete', 3, 4, 171, '', '', '', 1),
        (177, '通知发布', 'system:announce:publish', 3, 5, 171, '', '', '', 1),
        (178, '聊天查询', 'system:chat:query', 3, 1, 172, '', '', '', 1),
        (180, '定时任务', 'monitor:job:list', 2, 3, 100, '/monitor/job', 'Timer', 'monitor/job/index', 1),
        (181, '任务查询', 'monitor:job:query', 3, 1, 180, '', '', '', 1),
        (182, '任务新增', 'monitor:job:add', 3, 2, 180, '', '', '', 1),
        (183, '任务编辑', 'monitor:job:edit', 3, 3, 180, '', '', '', 1),
        (184, '任务删除', 'monitor:job:delete', 3, 4, 180, '', '', '', 1),
        (164, '代码生成', 'tool:gen:list', 2, 2, 150, '/tool/gen', 'SetUp', 'tool/gen/index', 1),
        (165, '代码生成查询', 'tool:gen:query', 3, 1, 164, '', '', '', 1),
        (166, '代码生成导入', 'tool:gen:import', 3, 2, 164, '', '', '', 1),
        (167, '代码生成修改', 'tool:gen:edit', 3, 3, 164, '', '', '', 1),
        (168, '代码生成删除', 'tool:gen:remove', 3, 4, 164, '', '', '', 1),
        (169, '代码生成预览', 'tool:gen:preview', 3, 5, 164, '', '', '', 1),
        (179, '代码生成执行', 'tool:gen:code', 3, 6, 164, '', '', '', 1)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name), permission = VALUES(permission), type = VALUES(type),
            sort = VALUES(sort), parent_id = VALUES(parent_id), path = VALUES(path),
            icon = VALUES(icon), component = VALUES(component), status = VALUES(status);
        SELECT '[OK] builtin menu sync applied' AS result;
    END IF;
END$$
DELIMITER ;
CALL sp_wu_admin_sync_builtin_menus();
DROP PROCEDURE IF EXISTS sp_wu_admin_sync_builtin_menus;

-- [附录·任务] 表 / 菜单 / 内置任务补建（正文 §7.3 已全量；含历史任务迁移）
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
    duration_ms BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_name (job_name),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务日志表';

-- [附录·任务] sys_job_log 增加毫秒级耗时（#16；旧表补列）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_log'
      AND COLUMN_NAME = 'duration_ms'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_job_log ADD COLUMN duration_ms BIGINT DEFAULT NULL COMMENT ''执行耗时(毫秒)'' AFTER stop_time',
    'SELECT ''duration_ms exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(180, '定时任务', 'monitor:job:list', 2, 3, 100, '/monitor/job', 'Timer', 'monitor/job/index', 1),
(181, '任务查询', 'monitor:job:query', 3, 1, 180, '', '', '', 1),
(182, '任务新增', 'monitor:job:add', 3, 2, 180, '', '', '', 1),
(183, '任务编辑', 'monitor:job:edit', 3, 3, 180, '', '', '', 1),
(184, '任务删除', 'monitor:job:delete', 3, 4, 180, '', '', '', 1),
(185, '缓存监控', 'monitor:cache:list', 2, 4, 100, '/monitor/cache', 'Coin', 'monitor/cache/index', 1),
(188, '缓存查询', 'monitor:cache:query', 3, 1, 185, '', '', '', 1),
(186, '缓存删除', 'monitor:cache:delete', 3, 2, 185, '', '', '', 1),
(187, '服务监控', 'monitor:server:list', 2, 5, 100, '/monitor/server', 'Cpu', 'monitor/server/index', 1),
(189, '服务监控查询', 'monitor:server:query', 3, 1, 187, '', '', '', 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 180), (1, 181), (1, 182), (1, 183), (1, 184), (1, 185), (1, 186), (1, 187), (1, 188), (1, 189);

-- 移除已废弃的内置任务；迁移旧版字典/配置缓存任务为聊天清理
DELETE FROM sys_job WHERE invoke_target IN (
    'systemJobTask.flushApiAccessLogs',
    'sampleJobTask.heartbeat'
);

-- 迁移旧版废弃/缓存类内置任务（按 invoke_target 匹配，不强制改 id=2/3）
UPDATE sys_job SET
    job_name = '私聊消息清理',
    invoke_target = 'systemJobTask.purgeOldChatMessages',
    cron_expression = '0 0 3 * * ?',
    remark = '清理超过 180 天的私聊记录'
WHERE invoke_target = 'systemJobTask.refreshDictCache';

UPDATE sys_job SET
    job_name = '群聊消息清理',
    invoke_target = 'systemJobTask.purgeOldGroupChatMessages',
    cron_expression = '0 10 3 * * ?',
    remark = '清理超过 180 天的群聊记录'
WHERE invoke_target = 'systemJobTask.refreshConfigCache';

-- 内置 6 项：仅首次插入（INSERT IGNORE），不覆盖管理员已修改的 cron/status
INSERT IGNORE INTO sys_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, remark) VALUES
(1, '过期日志归档清理', 'SYSTEM', 'systemJobTask.purgeExpiredLogs', '0 30 2 * * ?', 3, 1, 0, '清理超保留期的操作/登录/API访问日志'),
(2, '私聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldChatMessages', '0 0 3 * * ?', 3, 1, 0, '清理超过 180 天的私聊记录'),
(3, '群聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldGroupChatMessages', '0 10 3 * * ?', 3, 1, 0, '清理超过 180 天的群聊记录'),
(4, '调度日志清理', 'SYSTEM', 'systemJobTask.purgeExpiredJobLogs', '0 0 4 ? * SUN', 3, 1, 0, '清理 30 天前的 Quartz 调度执行日志'),
(5, '已读通知清理', 'SYSTEM', 'systemJobTask.purgeReadNotices', '0 15 3 * * ?', 3, 1, 0, '清理已读且超过 90 天的站内通知'),
(6, '工单回收站清理', 'SYSTEM', 'systemJobTask.purgeTicketRecycleBin', '0 30 3 * * ?', 3, 1, 0, '彻底删除回收站中超过 30 天的工单');

-- [附录·字典] 工单/审批业务字典（type 4～7）
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(4, '工单状态', 'sys_ticket_status', 1, '工单流转状态'),
(5, '工单优先级', 'sys_ticket_priority', 1, '工单优先级'),
(6, '审批类型', 'sys_approval_form_type', 1, '审批单业务类型'),
(7, '审批状态', 'sys_approval_status', 1, '审批单流转状态')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), remark = VALUES(remark);

-- 旧库可能因重复跑附录产生重复 dict_data：去重（保留 id 较小的一条）后补唯一索引
-- 注意：若重复项中较新 id 才是正确文案，会被删掉；仅用于加索引前的清理，正常库无重复则 no-op
DELETE d1 FROM sys_dict_data d1
INNER JOIN sys_dict_data d2
    ON d1.dict_type = d2.dict_type
    AND d1.dict_value = d2.dict_value
    AND d1.id > d2.id;

SET @uk_dict_cnt := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_dict_data'
      AND index_name = 'uk_dict_type_value'
);
SET @uk_dict_ddl := IF(
    @uk_dict_cnt = 0,
    'ALTER TABLE sys_dict_data ADD UNIQUE INDEX uk_dict_type_value (dict_type, dict_value)',
    'SELECT 1'
);
PREPARE uk_dict_stmt FROM @uk_dict_ddl;
EXECUTE uk_dict_stmt;
DEALLOCATE PREPARE uk_dict_stmt;

INSERT INTO sys_dict_data (dict_type, sort, dict_label, dict_value, list_class, is_default, status) VALUES
('sys_ticket_status', 1, '待处理', 'OPEN', 'info', 1, 1),
('sys_ticket_status', 2, '处理中', 'IN_PROGRESS', 'warning', 0, 1),
('sys_ticket_status', 3, '已解决', 'RESOLVED', 'success', 0, 1),
('sys_ticket_status', 4, '已关闭', 'CLOSED', 'danger', 0, 1),
('sys_ticket_priority', 1, '低', 'LOW', 'info', 0, 1),
('sys_ticket_priority', 2, '中', 'MEDIUM', 'success', 1, 1),
('sys_ticket_priority', 3, '高', 'HIGH', 'warning', 0, 1),
('sys_ticket_priority', 4, '紧急', 'URGENT', 'danger', 0, 1),
('sys_approval_form_type', 1, '通用', 'GENERAL', 'info', 1, 1),
('sys_approval_form_type', 2, '请假', 'LEAVE', 'primary', 0, 1),
('sys_approval_form_type', 3, '采购', 'PURCHASE', 'warning', 0, 1),
('sys_approval_form_type', 4, '报销', 'REIMBURSE', 'success', 0, 1),
('sys_approval_form_type', 5, '用印', 'SEAL', 'danger', 0, 1),
('sys_approval_form_type', 6, '合同', 'CONTRACT', 'info', 0, 1),
('sys_approval_form_type', 7, '注册审核', 'REGISTER', 'warning', 0, 1),
('sys_approval_status', 1, '待审批', 'SUBMITTED', 'warning', 1, 1),
('sys_approval_status', 2, '已通过', 'APPROVED', 'success', 0, 1),
('sys_approval_status', 3, '已驳回', 'REJECTED', 'danger', 0, 1),
('sys_approval_status', 4, '已归档', 'ARCHIVED', 'info', 0, 1)
ON DUPLICATE KEY UPDATE
    dict_label = VALUES(dict_label),
    list_class = VALUES(list_class),
    is_default = VALUES(is_default),
    sort = VALUES(sort);

-- [附录·配置] security 分组 + isConcurrent 补全
INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('security', '安全配置', '{"disableDevtool":false,"isConcurrent":false}', '前端安全与会话：禁止调试、禁止多端同时在线')
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    remark = VALUES(remark);

UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.isConcurrent', CAST(false AS JSON))
WHERE group_code = 'security'
  AND JSON_EXTRACT(config_value, '$.isConcurrent') IS NULL;

-- [附录·短信] 配置分组 + sys_sms_log 表（正文 §15b 已全量）
INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('sms', '短信配置', '{"enabled":false,"provider":"aliyunAuth","accessKeyId":"","accessKeySecret":"","signName":"","tencentAppId":"","templateVerifyCode":"100001","templateModifyPhone":"100002","templateResetPassword":"100003","templateBindPhone":"100004","templateVerifyBindPhone":"100005","schemeName":"","codeExpireMinutes":5}', '阿里云短信认证/腾讯云')
ON DUPLICATE KEY UPDATE group_name = VALUES(group_name), remark = VALUES(remark);

CREATE TABLE IF NOT EXISTS sys_sms_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    content VARCHAR(500) DEFAULT NULL COMMENT '短信内容/验证码',
    sms_type VARCHAR(20) DEFAULT 'verify_code' COMMENT 'verify_code / notice / marketing',
    template_id VARCHAR(50) DEFAULT NULL COMMENT '模板ID',
    template_params VARCHAR(500) DEFAULT NULL COMMENT '模板参数 JSON',
    provider VARCHAR(20) DEFAULT NULL COMMENT 'aliyun / tencent / console',
    status TINYINT DEFAULT 0 COMMENT '0-发送中 1-成功 2-失败',
    result_msg VARCHAR(500) DEFAULT NULL COMMENT '结果信息',
    biz_id VARCHAR(100) DEFAULT NULL COMMENT '服务商消息ID',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    biz_type VARCHAR(30) DEFAULT NULL COMMENT '业务类型',
    ip VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_phone_create_time (phone, create_time),
    INDEX idx_create_time (create_time),
    INDEX idx_send_time (send_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信发送记录表';

-- [附录·短信] provider 迁移：aliyun → aliyunAuth
UPDATE sys_config_group
SET config_value = JSON_SET(
        JSON_SET(
                JSON_SET(config_value, '$.provider', 'aliyunAuth'),
                '$.schemeName', IFNULL(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.schemeName')), '')
        ),
        '$.codeExpireMinutes',
        IFNULL(JSON_EXTRACT(config_value, '$.codeExpireMinutes'), CAST(5 AS JSON))
    )
WHERE group_code = 'sms'
  AND (
    JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.provider')) = 'aliyun'
        OR JSON_EXTRACT(config_value, '$.codeExpireMinutes') IS NULL
    );

-- [附录·短信] 赠送模板字段 100001～100005
UPDATE sys_config_group
SET config_value = JSON_SET(
        JSON_SET(
                JSON_SET(
                        JSON_SET(
                                JSON_SET(config_value,
                                        '$.templateVerifyCode',
                                        IFNULL(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.templateVerifyCode')), ''), '100001')
                                ),
                                '$.templateModifyPhone',
                                IFNULL(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.templateModifyPhone')), ''), '100002')
                        ),
                        '$.templateResetPassword',
                        IFNULL(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.templateResetPassword')), ''), '100003')
                ),
                '$.templateBindPhone',
                IFNULL(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.templateBindPhone')), ''), '100004')
        ),
        '$.templateVerifyBindPhone',
        IFNULL(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.templateVerifyBindPhone')), ''), '100005')
    )
WHERE group_code = 'sms';

-- [附录·登录] 短信登录独立开关 smsLoginEnabled（与原 captchaType=sms 迁移）
UPDATE sys_config_group
SET config_value = JSON_SET(
        JSON_SET(config_value, '$.smsLoginEnabled', CAST(true AS JSON)),
        '$.captchaType', 'image'
    )
WHERE group_code = 'login'
  AND JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')) = 'sms';

UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.smsLoginEnabled', CAST(false AS JSON))
WHERE group_code = 'login'
  AND JSON_EXTRACT(config_value, '$.smsLoginEnabled') IS NULL;

-- [附录·登录] 短信发送前滑块验证 smsLoginSliderCaptchaEnabled
UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.smsLoginSliderCaptchaEnabled', CAST(false AS JSON))
WHERE group_code = 'login'
  AND JSON_EXTRACT(config_value, '$.smsLoginSliderCaptchaEnabled') IS NULL;

-- [附录·登录] IP 锁定阈值 maxRetryCountIp
UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.maxRetryCountIp', CAST(20 AS JSON)),
    remark = '验证码 image/slider；smsLoginEnabled 短信登录；smsLoginSliderCaptchaEnabled 短信发送前滑块；maxRetryCount 账号锁定阈值；maxRetryCountIp IP 锁定阈值'
WHERE group_code = 'login'
  AND JSON_EXTRACT(config_value, '$.maxRetryCountIp') IS NULL;

-- [附录·限流] 短信发送防刷字段（IP/间隔/日上限）
UPDATE sys_config_group
SET config_value = JSON_SET(
        JSON_SET(
                JSON_SET(
                        JSON_SET(config_value,
                                '$.smsPerIpMinute',
                                IFNULL(JSON_EXTRACT(config_value, '$.smsPerIpMinute'), CAST(5 AS JSON))
                        ),
                        '$.smsSendIntervalSeconds',
                        IFNULL(JSON_EXTRACT(config_value, '$.smsSendIntervalSeconds'), CAST(60 AS JSON))
                ),
                '$.smsPerPhoneDaily',
                IFNULL(JSON_EXTRACT(config_value, '$.smsPerPhoneDaily'), CAST(10 AS JSON))
        ),
        '$.smsPerIpDaily',
        IFNULL(JSON_EXTRACT(config_value, '$.smsPerIpDaily'), CAST(30 AS JSON))
    )
WHERE group_code = 'rateLimit';

-- [附录·索引] 通用性能索引（sp_add_index_if_missing，已存在则跳过）
DROP PROCEDURE IF EXISTS sp_add_index_if_missing;

DELIMITER $$

CREATE PROCEDURE sp_add_index_if_missing(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;

    IF v_cnt = 0 THEN
        SET @ddl_sql = p_ddl;
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[OK] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

DELIMITER ;

CALL sp_add_index_if_missing('sys_dict_data', 'idx_dict_type_status_deleted',
    'ALTER TABLE sys_dict_data ADD INDEX idx_dict_type_status_deleted (dict_type, status, deleted, sort)');

CALL sp_add_index_if_missing('sys_user', 'idx_deleted_status',
    'ALTER TABLE sys_user ADD INDEX idx_deleted_status (deleted, status)');

CALL sp_add_index_if_missing('sys_user_post', 'uk_user_post',
    'ALTER TABLE sys_user_post ADD UNIQUE INDEX uk_user_post (user_id, post_id)');

CALL sp_add_index_if_missing('sys_oper_log', 'idx_oper_time_status',
    'ALTER TABLE sys_oper_log ADD INDEX idx_oper_time_status (oper_time, status)');

-- [附录·群成员] add9：免打扰、群公告已读时间（可重复执行）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_chat_group_member'
      AND COLUMN_NAME = 'notify_muted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_chat_group_member ADD COLUMN notify_muted TINYINT DEFAULT 0 COMMENT ''0正常 1免打扰仅@提醒'' AFTER muted',
    'SELECT ''notify_muted exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_chat_group_member'
      AND COLUMN_NAME = 'announcement_read_time'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_chat_group_member ADD COLUMN announcement_read_time DATETIME NULL COMMENT ''群公告已读时间'' AFTER notify_muted',
    'SELECT ''announcement_read_time exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CALL sp_add_index_if_missing('sys_chat_group_member', 'idx_user_id',
    'ALTER TABLE sys_chat_group_member ADD INDEX idx_user_id (user_id)');

CALL sp_add_index_if_missing('sys_chat_message', 'idx_receiver_unread',
    'ALTER TABLE sys_chat_message ADD INDEX idx_receiver_unread (receiver_id, is_read, sender_id)');

CALL sp_add_index_if_missing('sys_notice', 'idx_user_read_deleted',
    'ALTER TABLE sys_notice ADD INDEX idx_user_read_deleted (user_id, read_status, deleted)');

CALL sp_add_index_if_missing('sys_approval_form', 'idx_type_applicant_status',
    'ALTER TABLE sys_approval_form ADD INDEX idx_type_applicant_status (form_type, applicant_user_id, status, deleted)');

CALL sp_add_index_if_missing('sys_ticket', 'idx_deleted_status_time',
    'ALTER TABLE sys_ticket ADD INDEX idx_deleted_status_time (deleted, status, create_time)');

CALL sp_add_index_if_missing('sys_file', 'idx_group_time',
    'ALTER TABLE sys_file ADD INDEX idx_group_time (group_id, create_time)');

CALL sp_add_index_if_missing('sys_api_access_log', 'idx_start_time_success',
    'ALTER TABLE sys_api_access_log ADD INDEX idx_start_time_success (start_time, success)');

-- [附录·索引] 清理任务/短信日志时间索引 + 冗余单列索引清理
CALL sp_add_index_if_missing('sys_chat_message', 'idx_send_time',
    'ALTER TABLE sys_chat_message ADD INDEX idx_send_time (send_time)');

CALL sp_add_index_if_missing('sys_chat_group_message', 'idx_send_time',
    'ALTER TABLE sys_chat_group_message ADD INDEX idx_send_time (send_time)');

CALL sp_add_index_if_missing('sys_notice', 'idx_read_create_time',
    'ALTER TABLE sys_notice ADD INDEX idx_read_create_time (read_status, create_time)');

CALL sp_add_index_if_missing('sys_ticket', 'idx_deleted_update_time',
    'ALTER TABLE sys_ticket ADD INDEX idx_deleted_update_time (deleted, update_time)');

CALL sp_add_index_if_missing('sys_sms_log', 'idx_create_time',
    'ALTER TABLE sys_sms_log ADD INDEX idx_create_time (create_time)');

CALL sp_add_index_if_missing('sys_sms_log', 'idx_phone_create_time',
    'ALTER TABLE sys_sms_log ADD INDEX idx_phone_create_time (phone, create_time)');

DROP PROCEDURE IF EXISTS sp_add_index_if_missing;

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;

DELIMITER $$

CREATE PROCEDURE sp_drop_index_if_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;

    IF v_cnt > 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[DROP] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

DELIMITER ;

CALL sp_drop_index_if_exists('sys_notice', 'idx_user_read_status');
CALL sp_drop_index_if_exists('sys_sms_log', 'idx_phone');

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;

-- [附录·菜单] add1：在线用户查询按钮 190（可重复执行）
INSERT IGNORE INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(190, '在线用户查询', 'monitor:online:query', 3, 1, 103, '', '', '', 1);

UPDATE sys_menu
SET name = '在线用户查询',
    permission = 'monitor:online:query',
    type = 3,
    sort = 1,
    parent_id = 103,
    status = 1
WHERE id = 190
  AND (permission IS NULL OR permission = '' OR permission <> 'monitor:online:query');

UPDATE sys_menu SET sort = 2 WHERE id = 104 AND parent_id = 103 AND sort = 1;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 190);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 190
FROM sys_role_menu rm
WHERE rm.menu_id = 103;

-- [附录·菜单] add2：回收中心 query/restore/delete 191-193（可重复执行）
INSERT IGNORE INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(191, '回收中心查询', 'system:recycle:query', 3, 1, 163, '', '', '', 1),
(192, '回收中心恢复', 'system:recycle:restore', 3, 2, 163, '', '', '', 1),
(193, '回收中心删除', 'system:recycle:delete', 3, 3, 163, '', '', '', 1);

UPDATE sys_menu
SET name = '回收中心查询', permission = 'system:recycle:query', type = 3, sort = 1, parent_id = 163, status = 1
WHERE id = 191 AND (permission IS NULL OR permission = '' OR permission <> 'system:recycle:query');

UPDATE sys_menu
SET name = '回收中心恢复', permission = 'system:recycle:restore', type = 3, sort = 2, parent_id = 163, status = 1
WHERE id = 192 AND (permission IS NULL OR permission = '' OR permission <> 'system:recycle:restore');

UPDATE sys_menu
SET name = '回收中心删除', permission = 'system:recycle:delete', type = 3, sort = 3, parent_id = 163, status = 1
WHERE id = 193 AND (permission IS NULL OR permission = '' OR permission <> 'system:recycle:delete');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 191), (1, 192), (1, 193);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 191
FROM sys_role_menu rm
WHERE rm.menu_id = 163;

-- [附录·AI] add5：AI wu助手（sys_ai_model / sys_ai_chat_log + 「AI 管理」菜单，可重复执行）
CREATE TABLE IF NOT EXISTS sys_ai_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模型配置ID',
    name VARCHAR(100) NOT NULL COMMENT '配置名称（展示用，如「DeepSeek 官方」）',
    provider VARCHAR(32) NOT NULL COMMENT '供应商: deepseek/openai/qwen/kimi',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称（如 deepseek-chat / qwen-plus）',
    base_url VARCHAR(255) NOT NULL COMMENT 'API 基础地址（不含 /chat/completions）',
    api_key VARCHAR(1024) DEFAULT '' COMMENT 'API 密钥（SM4-CBC 加密存储）',
    temperature DECIMAL(3,2) DEFAULT 0.70 COMMENT '采样温度 0~2',
    max_tokens INT DEFAULT 4096 COMMENT '单次回复最大 Token 数',
    system_prompt VARCHAR(2000) DEFAULT NULL COMMENT '系统提示词（角色设定）',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认模型 0:否 1:是（全局唯一）',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_provider (provider),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型供应商配置表';

CREATE TABLE IF NOT EXISTS sys_ai_chat_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT DEFAULT 0 COMMENT '提问用户ID',
    username VARCHAR(64) DEFAULT '' COMMENT '提问用户名',
    conversation_id VARCHAR(64) DEFAULT '' COMMENT '会话ID（前端生成，串联多轮对话）',
    model_id BIGINT DEFAULT 0 COMMENT '模型配置ID',
    provider VARCHAR(32) DEFAULT '' COMMENT '供应商',
    model_name VARCHAR(100) DEFAULT '' COMMENT '模型名称',
    question TEXT COMMENT '用户提问（脱敏后）',
    answer MEDIUMTEXT COMMENT 'AI 回答',
    prompt_tokens INT DEFAULT 0 COMMENT '提问 Token 消耗',
    completion_tokens INT DEFAULT 0 COMMENT '回答 Token 消耗',
    total_tokens INT DEFAULT 0 COMMENT '总 Token 消耗',
    duration_ms BIGINT DEFAULT 0 COMMENT '耗时（毫秒）',
    chat_status TINYINT DEFAULT 1 COMMENT '结果 1:成功 0:失败 2:用户中断',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    source VARCHAR(20) DEFAULT 'pc' COMMENT '来源终端 pc/mobile',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user_id (user_id),
    INDEX idx_model_id (model_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话日志审计表';

-- 菜单：顶级目录「AI 管理」（id=210）+ AI 模型配置 / AI 对话日志 + 按钮权限
INSERT IGNORE INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(210, 'AI 管理', '', 1, 8, 0, '/ai', 'MagicStick', '', 1),
(200, 'AI 模型配置', 'system:ai-model:list', 2, 1, 210, '/ai/model', 'MagicStick', 'ai/model/index', 1),
(201, 'AI模型新增', 'system:ai-model:create', 3, 1, 200, '', '', '', 1),
(202, 'AI模型修改', 'system:ai-model:update', 3, 2, 200, '', '', '', 1),
(203, 'AI模型删除', 'system:ai-model:delete', 3, 3, 200, '', '', '', 1),
(204, 'AI模型测试', 'system:ai-model:test', 3, 4, 200, '', '', '', 1),
(205, 'AI 对话日志', 'system:ai-log:list', 2, 2, 210, '/ai/log', 'ChatDotRound', 'ai/log/index', 1),
(206, 'AI日志删除', 'system:ai-log:delete', 3, 1, 205, '', '', '', 1);

-- 存量库迁移：早期版本曾将两个菜单挂在「系统管理」id=1 下，统一迁至「AI 管理」（可重复执行）
UPDATE sys_menu SET parent_id = 210, sort = 1, path = '/ai/model', component = 'ai/model/index' WHERE id = 200;
UPDATE sys_menu SET parent_id = 210, sort = 2, path = '/ai/log', component = 'ai/log/index' WHERE id = 205;

-- 顶级排序：「AI 管理」排在「开发工具」之前（可重复执行）
UPDATE sys_menu SET sort = 8 WHERE id = 210;
UPDATE sys_menu SET sort = 9 WHERE id = 150;

-- 超管默认拥有全部 AI 菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 210), (1, 200), (1, 201), (1, 202), (1, 203), (1, 204), (1, 205), (1, 206);

