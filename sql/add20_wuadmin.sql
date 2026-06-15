-- =============================================================================
-- add20_wuadmin.sql  生产/服务器增量补丁 #20（可重复执行）
-- =============================================================================
-- 缓存监控 / 服务监控：补齐「查询」按钮权限，与操作日志、API 访问统计等模块一致。
-- 已有「缓存监控」「服务监控」菜单的角色自动补授对应查询权限。
--
-- 用法: mysql -u wuadmin -p wuadmin < sql/add20_wuadmin.sql
-- 执行后请重新登录。
-- =============================================================================

USE `wuadmin`;

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (188, '缓存查询', 'monitor:cache:query', 3, 1, 185, '', '', '', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    status = VALUES(status);

UPDATE sys_menu SET sort = 2 WHERE id = 186;

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (189, '服务监控查询', 'monitor:server:query', 3, 1, 187, '', '', '', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    status = VALUES(status);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 188), (1, 189);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 188 FROM sys_role_menu WHERE menu_id = 185;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 189 FROM sys_role_menu WHERE menu_id = 187;

SELECT '[OK] add20_wuadmin.sql finished (#20 cache/server query permissions)' AS result;
