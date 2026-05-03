-- =============================================
-- Admin Platform 统一数据库初始化脚本
-- 数据库名: RBAC1
-- 包含: 用户、角色、菜单、部门、登录日志
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS RBAC1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE RBAC1;

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
    INDEX idx_dept_id (dept_id)
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
    INDEX idx_assignee_user_id (assignee_user_id)
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
    INDEX idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

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
    INDEX idx_approver_user_id (approver_user_id)
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
-- 初始化数据
-- =============================================

-- 初始化部门
INSERT INTO sys_dept (id, name, parent_id, sort, status, leader_name) VALUES
(1, '总公司', 0, 0, 1, '管理员'),
(2, '研发部', 1, 1, 1, '张三'),
(3, '市场部', 1, 2, 1, '李四'),
(4, '财务部', 1, 3, 1, '王五');

-- 初始化用户 (密码为 admin123，BCrypt加密)
INSERT INTO sys_user (id, username, password, nickname, mobile, email, status, dept_id) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', '13800138000', 'admin@admin.cn', 1, 1),
(2, 'zhangsan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', '13800138001', 'zhangsan@admin.cn', 1, 2);

-- 初始化角色
INSERT INTO sys_role (id, name, code, sort, status, remark) VALUES
(1, '超级管理员', 'super_admin', 1, 1, '超级管理员，拥有所有权限'),
(2, '普通用户', 'user', 2, 1, '普通用户角色');

-- 初始化菜单
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
-- 系统管理目录
(1, '系统管理', '', 1, 1, 0, '/system', 'system', '', 1),
-- 用户管理
(2, '用户管理', 'system:user:list', 2, 1, 1, '/system/user', 'user', 'system/user/index', 1),
-- 角色管理
(3, '角色管理', 'system:role:list', 2, 2, 1, '/system/role', 'peoples', 'system/role/index', 1),
-- 菜单管理
(4, '菜单管理', 'system:menu:list', 2, 3, 1, '/system/menu', 'tree-table', 'system/menu/index', 1),
-- 部门管理
(5, '部门管理', 'system:dept:list', 2, 4, 1, '/system/dept', 'tree', 'system/dept/index', 1),
-- 登录日志
(6, '登录日志', 'system:loginLog:list', 2, 5, 1, '/system/login-log', 'document', 'system/login-log/index', 1),
-- 工单管理
(7, '工单管理', 'system:ticket:list', 2, 6, 1, '/system/ticket', 'Document', 'system/ticket/index', 1),
-- 业务中心目录
(8, '业务中心', '', 1, 2, 0, '/business', 'Suitcase', '', 1),
-- 审批单中心
(9, '审批单中心', 'system:approval:list', 2, 1, 8, '/system/approval', 'Checked', 'system/approval/index', 1),
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
(74, '审批单删除', 'system:approval:delete', 3, 5, 9, '', '', '', 1);

-- 初始化用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin 拥有超级管理员角色
(2, 2); -- zhangsan 拥有普通用户角色

-- 初始化角色菜单关联 (超级管理员拥有所有菜单权限)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(1, 10), (1, 11), (1, 12), (1, 13),
(1, 20), (1, 21), (1, 22), (1, 23),
(1, 30), (1, 31), (1, 32), (1, 33),
(1, 40), (1, 41), (1, 42), (1, 43),
(1, 50), (1, 51), (1, 52),
(1, 60), (1, 61), (1, 62), (1, 63), (1, 64), (1, 65),
(1, 70), (1, 71), (1, 72), (1, 73), (1, 74);

-- 普通用户只有查询权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9),
(2, 10), (2, 20), (2, 30), (2, 40), (2, 50), (2, 60), (2, 70), (2, 71), (2, 72), (2, 73);
