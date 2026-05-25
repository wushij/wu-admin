-- =============================================
-- Admin Platform 统一数据库脚本（唯一入口）
-- 数据库名: wu-admin
--
-- 【全新安装】执行本文件全文即可（建库、建表、初始数据）。
-- 【已有库升级】若表已存在，可只执行文末「附录：已有库升级」段（可重复执行）。
--
-- 已有库增量：add1.sql～add3.sql（业务/消息）、add4.sql（索引）、add5.sql（安全配置）、add6.sql（组织示例数据）；
-- 或执行文末「附录：已有库升级」段（配置/菜单/消息表/组织数据等，可重复执行）。
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

-- =============================================
-- 1. 用户表
-- =============================================
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

-- =============================================
-- 2. 角色表
-- =============================================
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

-- =============================================
-- 3. 菜单表
-- =============================================
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

-- =============================================
-- 4. 部门表
-- =============================================
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    ancestors VARCHAR(500) DEFAULT '' COMMENT '祖级列表，如 0,1,5',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    leader_name VARCHAR(50) DEFAULT NULL COMMENT '负责人',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- =============================================
-- 4.1 岗位表
-- =============================================
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

-- =============================================
-- 5. 用户角色关联表
-- =============================================
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- =============================================
-- 6. 角色菜单关联表
-- =============================================
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- =============================================
-- 7. 登录日志表
-- =============================================
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

-- =============================================
-- 7.0 字典类型 / 字典数据
-- =============================================
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
    KEY idx_dict_type (dict_type),
    KEY idx_dict_type_status_deleted (dict_type, status, deleted, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- =============================================
-- 7.1 操作日志表
-- =============================================
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

-- =============================================
-- 8. 工单表
-- =============================================
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
    INDEX idx_deleted_status_time (deleted, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- =============================================
-- 9. 工单评论表
-- =============================================
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

-- =============================================
-- 10. 工单附件表
-- =============================================
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

-- =============================================
-- 11. 站内消息表
-- =============================================
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
    INDEX idx_user_read_status (user_id, read_status),
    INDEX idx_user_read_deleted (user_id, read_status, deleted),
    INDEX idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- =============================================
-- 11b. 消息中心（系统通知 + 即时聊天）
-- 保留 sys_notice 为业务收件箱；sys_announce 为管理员发布的广播通知
-- =============================================
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
    INDEX idx_pair_time (sender_id, receiver_id, send_time)
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
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_time (group_id, send_time)
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

-- =============================================
-- 12. 审批单表
-- =============================================
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

-- =============================================
-- 13. 审批记录表
-- =============================================
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

-- =============================================
-- 14. API 访问统计日志表
-- =============================================
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

-- =============================================
-- 15. 文件管理
-- =============================================
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
    PRIMARY KEY (id),
    INDEX idx_group_id (group_id),
    INDEX idx_create_time (create_time),
    INDEX idx_group_time (group_id, create_time)
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

-- =============================================
-- 系统配置分组表
-- =============================================
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

-- =============================================
-- 初始化数据
-- =============================================

-- 初始化部门
INSERT INTO sys_dept (id, name, parent_id, ancestors, sort, status, leader_name) VALUES
(1, '总公司', 0, '0', 0, 1, '管理员'),
(2, '研发部', 1, '0,1', 1, 1, '张三'),
(3, '市场部', 1, '0,1', 2, 1, '李四'),
(4, '财务部', 1, '0,1', 3, 1, '王五'),
(5, '实训部', 1, '0,1', 4, 1, NULL),
(6, '人事部', 1, '0,1', 5, 1, NULL),
(7, '运维部', 1, '0,1', 6, 1, NULL),
(8, '产品部', 1, '0,1', 7, 1, NULL),
(9, '客服部', 1, '0,1', 8, 1, NULL),
(10, '前端组', 2, '0,1,2', 1, 1, NULL),
(11, '后端组', 2, '0,1,2', 2, 1, NULL);

INSERT INTO sys_post (id, parent_id, post_code, post_name, sort, status, remark) VALUES
(1, 0, 'ceo', '总经理', 0, 1, '顶级岗位'),
(2, 0, 'dev', '研发工程师', 1, 1, ''),
(3, 2, 'dev_lead', '研发组长', 0, 1, '隶属研发工程师'),
(4, 0, 'qa', '测试', 2, 1, ''),
(5, 4, 'qa_lead', '测试组长', 0, 1, '隶属测试'),
(6, 0, 'market_spec', '市场专员', 3, 1, ''),
(7, 0, 'finance_mgr', '财务主管', 4, 1, ''),
(8, 0, 'train_lecturer', '实训讲师', 5, 1, ''),
(9, 0, 'hr_spec', '人事专员', 6, 1, ''),
(10, 0, 'ops_eng', '运维工程师', 7, 1, ''),
(11, 0, 'product_mgr', '产品经理', 8, 1, ''),
(12, 2, 'fe_dev', '前端开发', 1, 1, '隶属研发工程师'),
(13, 2, 'be_dev', '后端开发', 2, 1, '隶属研发工程师');

ALTER TABLE sys_dept AUTO_INCREMENT = 12;
ALTER TABLE sys_post AUTO_INCREMENT = 14;

-- 初始化用户 (密码为 admin123，BCrypt加密)
INSERT INTO sys_user (id, username, password, nickname, mobile, email, status, dept_id) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', '13800138000', 'admin@admin.cn', 1, 1),
(2, 'zhangsan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', '13800138001', 'zhangsan@admin.cn', 1, 2);

-- 初始化角色
INSERT INTO sys_role (id, name, code, sort, status, remark) VALUES
(1, '超级管理员', 'super_admin', 1, 1, '超级管理员，拥有所有权限'),
(2, '普通用户', 'user', 2, 1, '仅部分功能');

-- 初始化字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(1, '系统状态', 'sys_normal_disable', 1, '通用启用停用'),
(2, '用户性别', 'sys_user_sex', 1, '用户性别'),
(3, '是否', 'sys_yes_no', 1, '是或否');

INSERT INTO sys_dict_data (dict_type, sort, dict_label, dict_value, list_class, is_default, status) VALUES
('sys_normal_disable', 1, '启用', '1', 'success', 1, 1),
('sys_normal_disable', 2, '禁用', '0', 'danger', 0, 1),
('sys_user_sex', 1, '男', '1', 'primary', 0, 1),
('sys_user_sex', 2, '女', '2', 'danger', 0, 1),
('sys_user_sex', 3, '未知', '0', 'info', 1, 1),
('sys_yes_no', 1, '是', 'Y', 'success', 1, 1),
('sys_yes_no', 2, '否', 'N', 'info', 0, 1);

INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('site', '基础信息', '{"platformName":"Admin Platform","platformSubtitle":"统一运维 · 高效管控","loginWelcome":"Welcome","registerTitle":"Sign Up","copyright":""}', '平台展示名称与登录页文案'),
('session', '会话配置', '{"tokenExpireHours":24}', 'JWT 与 Redis 会话有效期（小时）'),
('file', '文件配置', '{"maxSizeMb":50,"allowedExtensions":"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov"}', '文件管理上传限制'),
('rateLimit', '接口限流', '{"captchaPerIpMinute":40,"loginPerIpMinute":30,"registerPerIpMinute":10}', '认证接口按 IP 限流'),
('login', '登录配置', '{"captchaEnabled":true,"captchaType":"image","rememberMe":true,"maxRetryCount":5,"lockTime":10}', '验证码类型 image=图片 slider=滑块'),
('register', '注册配置', '{"enabled":true,"captchaEnabled":true,"captchaType":"image","defaultRoleCode":"user","needAudit":false,"minPasswordLength":6}', '开放注册、验证码类型、默认角色、是否审核'),
('security', '安全配置', '{"disableDevtool":false,"isConcurrent":false}', '前端安全与会话：禁止调试、禁止多端同时在线');

-- 初始化菜单
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
-- 业务中心目录
(8, '业务中心', '', 1, 2, 0, '/business', 'Suitcase', '', 1),
-- 审批单中心
(9, '审批单中心', 'system:approval:list', 2, 1, 8, '/system/approval', 'Checked', 'system/approval/index', 1),
-- 工单管理（隶属业务中心）
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
(105, '文件管理', '', 1, 4, 0, '/file', 'Folder', '', 1),
(110, '文件列表', 'sys:file:list', 2, 1, 105, '/system/file', 'Document', 'system/file/index', 1),
(111, '文件查询', 'sys:file:query', 3, 1, 110, '', '', '', 1),
(112, '文件上传', 'sys:file:upload', 3, 2, 110, '', '', '', 1),
(113, '文件删除', 'sys:file:delete', 3, 3, 110, '', '', '', 1),
-- 系统监控目录
(100, '系统监控', '', 1, 3, 0, '/monitor', 'Monitor', '', 1),
-- API 访问统计
(101, 'API访问统计', 'monitor:apiAccess:list', 2, 1, 100, '/monitor/api-access', 'DataLine', 'monitor/api-access/index', 1),
(102, '访问统计查询', 'monitor:apiAccess:query', 3, 1, 101, '', '', '', 1),
-- 在线用户
(103, '在线用户', 'monitor:online:list', 2, 2, 100, '/monitor/online', 'User', 'monitor/online/index', 1),
(104, '在线用户强退', 'monitor:online:forceLogout', 3, 1, 103, '', '', '', 1),
-- 系统日志目录
(120, '系统日志', '', 1, 5, 0, '/log', 'Notebook', '', 1),
(121, '操作日志', 'system:operLog:list', 2, 1, 120, '/system/oper-log', 'EditPen', 'system/oper-log/index', 1),
(126, '操作日志查询', 'system:operLog:query', 3, 1, 121, '', '', '', 1),
(127, '操作日志删除', 'system:operLog:delete', 3, 2, 121, '', '', '', 1),
(128, '操作日志清空', 'system:operLog:clear', 3, 3, 121, '', '', '', 1),
-- 登录日志（隶属系统日志）
(6, '登录日志', 'system:loginLog:list', 2, 2, 120, '/system/login-log', 'Promotion', 'system/login-log/index', 1),
-- 开发工具
(150, '开发工具', '', 1, 6, 0, '/tool', 'Tools', '', 1),
(151, '接口文档', 'tool:apiDoc:view', 2, 1, 150, '/tool/api-doc', 'Document', '/doc.html', 1),
-- 消息中心
(170, '消息中心', '', 1, 7, 0, '/message', 'Bell', '', 1),
(171, '系统通知', 'system:announce:list', 2, 1, 170, '/message/notice', 'Notification', 'message/notice/index', 1),
(172, '即时聊天', 'system:chat:list', 2, 2, 170, '/message/chat', 'ChatDotRound', 'message/chat/index', 1),
(173, '通知查询', 'system:announce:query', 3, 1, 171, '', '', '', 1),
(174, '通知新增', 'system:announce:create', 3, 2, 171, '', '', '', 1),
(175, '通知修改', 'system:announce:update', 3, 3, 171, '', '', '', 1),
(176, '通知删除', 'system:announce:delete', 3, 4, 171, '', '', '', 1),
(177, '通知发布', 'system:announce:publish', 3, 5, 171, '', '', '', 1),
(178, '聊天查询', 'system:chat:query', 3, 1, 172, '', '', '', 1);

-- 初始化用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin 拥有超级管理员角色
(2, 2); -- zhangsan 拥有普通用户角色

-- 初始化角色菜单关联 (超级管理员拥有所有菜单权限)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 130), (1, 160), (1, 7), (1, 8), (1, 9),
(1, 6), (1, 120), (1, 121), (1, 126), (1, 127), (1, 128),
(1, 10), (1, 11), (1, 12), (1, 13),
(1, 20), (1, 21), (1, 22), (1, 23),
(1, 30), (1, 31), (1, 32), (1, 33),
(1, 40), (1, 41), (1, 42), (1, 43), (1, 44), (1, 45), (1, 46), (1, 47),
(1, 131), (1, 132), (1, 133), (1, 134), (1, 135), (1, 161), (1, 162),
(1, 50), (1, 51), (1, 52),
(1, 60), (1, 61), (1, 62), (1, 63), (1, 64), (1, 65),
(1, 70), (1, 71), (1, 72), (1, 73), (1, 74),
(1, 100), (1, 101), (1, 102), (1, 103), (1, 104),
(1, 105), (1, 110), (1, 111), (1, 112), (1, 113),
(1, 150), (1, 151),
(1, 170), (1, 171), (1, 172), (1, 173), (1, 174), (1, 175), (1, 176), (1, 177), (1, 178);

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

-- =============================================
-- 附录：已有库升级（可重复执行，全新安装执行亦无害）
-- 仅执行本段即可，无需其它 sql 文件
-- =============================================

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

INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('site', '基础信息', '{"platformName":"Admin Platform","platformSubtitle":"统一运维 · 高效管控","loginWelcome":"Welcome","registerTitle":"Sign Up","copyright":""}', '平台展示名称与登录页文案'),
('session', '会话配置', '{"tokenExpireHours":24}', 'JWT 与 Redis 会话有效期（小时）'),
('file', '文件配置', '{"maxSizeMb":50,"allowedExtensions":"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov"}', '文件管理上传限制'),
('rateLimit', '接口限流', '{"captchaPerIpMinute":40,"loginPerIpMinute":30,"registerPerIpMinute":10}', '认证接口按 IP 限流'),
('login', '登录配置', '{"captchaEnabled":true,"captchaType":"image","rememberMe":true,"maxRetryCount":5,"lockTime":10}', '验证码类型 image=图片 slider=滑块'),
('register', '注册配置', '{"enabled":true,"captchaEnabled":true,"captchaType":"image","defaultRoleCode":"user","needAudit":false,"minPasswordLength":6}', '开放注册、验证码类型、默认角色、是否审核'),
('security', '安全配置', '{"disableDevtool":false,"isConcurrent":false}', '前端安全与会话：禁止调试、禁止多端同时在线')
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    config_value = VALUES(config_value),
    remark = VALUES(remark);

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(160, '系统配置', 'system:config:list', 2, 6, 1, '/system/config', 'Tools', 'system/config/index', 1),
(161, '配置查询', 'system:config:query', 3, 1, 160, '', '', '', 1),
(162, '配置修改', 'system:config:update', 3, 2, 160, '', '', '', 1)
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

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 160), (1, 161), (1, 162);

-- 菜单图标
UPDATE sys_menu SET icon = 'UserFilled' WHERE id = 3 AND icon IN ('Key', 'key');
UPDATE sys_menu SET icon = 'Document' WHERE id = 151 AND icon IS NOT NULL AND icon <> 'Document';

-- 操作日志「查询」按钮（旧库可能缺失）
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(126, '操作日志查询', 'system:operLog:query', 3, 1, 121, '', '', '', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    status = VALUES(status);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 126);

-- 消息中心（已有库升级请执行 sql/add2.sql、sql/add3.sql，勿重复执行全量脚本）
-- 以下段落与 message_center.sql 内容一致，供全量安装时一并创建
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
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_time (group_id, send_time)
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
    name = VALUES(name), permission = VALUES(permission), type = VALUES(type),
    sort = VALUES(sort), parent_id = VALUES(parent_id), path = VALUES(path),
    icon = VALUES(icon), component = VALUES(component), status = VALUES(status);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 170), (1, 171), (1, 172), (1, 173), (1, 174), (1, 175), (1, 176), (1, 177), (1, 178),
(2, 170), (2, 172), (2, 178);

-- 操作日志操作人员：历史误存 userId 时回填为 username
UPDATE sys_oper_log o
INNER JOIN sys_user u ON u.id = CAST(o.oper_name AS UNSIGNED) AND u.deleted = 0
SET o.oper_name = u.username
WHERE o.oper_name REGEXP '^[0-9]+$';

-- 普通用户默认菜单与备注（执行后请普通用户重新登录）
UPDATE sys_role SET remark = '仅部分功能' WHERE id = 2;

DELETE FROM sys_role_menu WHERE role_id = 2;

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

-- 可选：为历史「待审核」用户补建注册审批单（无则跳过；审批人只查一次）
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
    CONCAT('{"bizType":"USER_REGISTER","userId":', u.id, ',"username":"', u.username, '","nickname":"', IFNULL(u.nickname, ''), '","mobile":"', IFNULL(u.mobile, ''), '"}'),
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

-- 通用状态字典文案与业务页一致（启用/禁用）
UPDATE sys_dict_data SET dict_label = '启用' WHERE dict_type = 'sys_normal_disable' AND dict_value = '1';
UPDATE sys_dict_data SET dict_label = '禁用' WHERE dict_type = 'sys_normal_disable' AND dict_value = '0';

-- ---------- 组织示例数据（已有库可单独执行 sql/add6.sql，与下文一致） ----------
INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '实训部', 1, '0,1', 4, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '实训部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '人事部', 1, '0,1', 5, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '人事部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '运维部', 1, '0,1', 6, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '运维部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '产品部', 1, '0,1', 7, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '产品部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '客服部', 1, '0,1', 8, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '客服部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '前端组', d.id, CONCAT(d.ancestors, ',', d.id), 1, 1, NULL
FROM sys_dept d
WHERE d.name = '研发部' AND d.parent_id = 1 AND d.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept c WHERE c.name = '前端组' AND c.parent_id = d.id AND c.deleted = 0)
LIMIT 1;

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '后端组', d.id, CONCAT(d.ancestors, ',', d.id), 2, 1, NULL
FROM sys_dept d
WHERE d.name = '研发部' AND d.parent_id = 1 AND d.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept c WHERE c.name = '后端组' AND c.parent_id = d.id AND c.deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'qa', '测试', 2, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'qa' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'qa_lead', '测试组长', 0, 1, '隶属测试'
FROM sys_post p
WHERE p.post_code = 'qa' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'qa_lead' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'market_spec', '市场专员', 3, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'market_spec' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'finance_mgr', '财务主管', 4, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'finance_mgr' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'train_lecturer', '实训讲师', 5, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'train_lecturer' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'hr_spec', '人事专员', 6, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'hr_spec' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'ops_eng', '运维工程师', 7, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'ops_eng' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'product_mgr', '产品经理', 8, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'product_mgr' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'fe_dev', '前端开发', 0, 1, '隶属研发工程师'
FROM sys_post p
WHERE p.post_code = 'dev' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'fe_dev' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'be_dev', '后端开发', 1, 1, '隶属研发工程师'
FROM sys_post p
WHERE p.post_code = 'dev' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'be_dev' AND deleted = 0)
LIMIT 1;

-- 已有 security 分组但缺少 isConcurrent 时补默认 false（与 add5.sql 一致）
UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.isConcurrent', CAST(false AS JSON))
WHERE group_code = 'security'
  AND JSON_EXTRACT(config_value, '$.isConcurrent') IS NULL;

-- ---------- 性能索引（已有库请单独执行 sql/add4.sql，勿重复执行下方 ALTER） ----------
