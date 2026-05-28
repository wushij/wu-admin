-- =============================================
-- add1.sql — 已有数据库增量升级（wu-admin）
-- 用途：从 RBAC1 等旧库迁移到 wu-admin 后，补菜单/配置/索引等
-- 说明：
--   1. 不含 DROP TABLE，不删业务数据
--   2. 可重复执行；索引若已存在会报 Duplicate key name，可忽略
--   3. 系统配置：仅新增缺失分组，已存在的分组不覆盖 config_value
-- 执行示例：
--   mysql -u root -p --default-character-set=utf8mb4 wu-admin < sql/add1.sql
-- =============================================

USE `wu-admin`;

SET NAMES utf8mb4;

-- ---------- 1. 系统配置分组表（缺则建表） ----------
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

-- 仅插入缺失分组；已存在的不改 config_value（保留你迁移后的配置）
INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('site', '基础信息', '{"platformName":"Admin Platform","platformSubtitle":"统一运维 · 高效管控","loginWelcome":"Welcome","registerTitle":"Sign Up","copyright":""}', '平台展示名称与登录页文案'),
('session', '会话配置', '{"tokenExpireHours":24}', '会话有效期（小时）'),
('file', '文件配置', '{"maxSizeMb":50,"allowedExtensions":"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov"}', '文件上传限制'),
('rateLimit', '接口限流', '{"captchaPerIpMinute":40,"loginPerIpMinute":30,"registerPerIpMinute":10}', '认证接口按 IP 限流'),
('login', '登录配置', '{"captchaEnabled":true,"captchaType":"image","rememberMe":true,"maxRetryCount":5,"lockTime":10}', '验证码 image/slider'),
('register', '注册配置', '{"enabled":true,"captchaEnabled":true,"defaultRoleCode":"user","needAudit":false,"minPasswordLength":6}', '注册与审核')
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    remark = VALUES(remark);

-- ---------- 2. 菜单与权限（补全缺失项） ----------
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

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(126, '操作日志查询', 'system:operLog:query', 3, 1, 121, '', '', '', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    status = VALUES(status);

-- 超级管理员：补系统配置、操作日志查询权限（不删已有授权）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 160), (1, 161), (1, 162), (1, 126);

-- 普通用户：仅追加系统配置查看（不 DELETE 原菜单，避免覆盖你自定义的权限）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2, 160), (2, 161);

UPDATE sys_menu SET icon = 'UserFilled' WHERE id = 3 AND icon IN ('Key', 'key');
UPDATE sys_menu SET icon = 'Document' WHERE id = 151 AND icon IS NOT NULL AND icon <> 'Document';

-- ---------- 3. 操作日志历史数据修正（可选） ----------
UPDATE sys_oper_log o
INNER JOIN sys_user u ON u.id = CAST(o.oper_name AS UNSIGNED) AND u.deleted = 0
SET o.oper_name = u.username
WHERE o.oper_name REGEXP '^[0-9]+$';

-- ---------- 4. 字典文案 ----------
UPDATE sys_dict_data SET dict_label = '启用' WHERE dict_type = 'sys_normal_disable' AND dict_value = '1';
UPDATE sys_dict_data SET dict_label = '禁用' WHERE dict_type = 'sys_normal_disable' AND dict_value = '0';

-- ---------- 5. 待审核用户补注册审批单（无待审核用户则 0 行） ----------
INSERT INTO sys_approval_form (form_no, form_type, title, content, status, applicant_user_id, approver_user_id, creator, updater)
SELECT
    CONCAT('RG', UNIX_TIMESTAMP(), LPAD(u.id, 4, '0')),
    'REGISTER',
    CONCAT('用户注册审核 - ', u.username),
    CONCAT('{"bizType":"USER_REGISTER","userId":', u.id, ',"username":"', u.username, '","nickname":"', IFNULL(u.nickname, ''), '","mobile":"', IFNULL(u.mobile, ''), '"}'),
    'SUBMITTED',
    u.id,
    (SELECT ur.user_id FROM sys_user_role ur
     INNER JOIN sys_role r ON r.id = ur.role_id AND r.code = 'super_admin' AND r.deleted = 0
     ORDER BY ur.user_id LIMIT 1),
    'system',
    'system'
FROM sys_user u
WHERE u.deleted = 0
  AND u.status = 2
  AND NOT EXISTS (
    SELECT 1 FROM sys_approval_form f
    WHERE f.deleted = 0
      AND f.form_type = 'REGISTER'
      AND f.applicant_user_id = u.id
      AND f.status = 'SUBMITTED'
  );

-- ---------- 6. 索引优化（Duplicate key name 表示已存在，可忽略） ----------
ALTER TABLE sys_dict_data ADD INDEX idx_dict_type_status_deleted (dict_type, status, deleted, sort);
ALTER TABLE sys_user ADD INDEX idx_deleted_status (deleted, status);
ALTER TABLE sys_user_post ADD UNIQUE INDEX uk_user_post (user_id, post_id);
ALTER TABLE sys_oper_log ADD INDEX idx_oper_time_status (oper_time, status);

-- 完成。请重启后端以刷新 Redis 配置/字典缓存。
