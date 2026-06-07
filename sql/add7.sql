-- =============================================================================
-- add7.sql  增量补丁 #7（可重复执行）
-- =============================================================================
-- 系统监控下新增「缓存监控」菜单（Redis INFO 图表 + SCAN 键管理）。
--
-- 用法: mysql -u root -p wu-admin < sql/add7.sql
-- 生产/服务器: mysql -u wuadmin -p wuadmin < sql/add6_7_wuadmin.sql
-- 执行后请重新登录；超管角色自动授权，其他角色需在菜单/角色中勾选。
-- =============================================================================

USE `wu-admin`;

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (185, '缓存监控', 'monitor:cache:list', 2, 4, 100, '/monitor/cache', 'Coin', 'monitor/cache/index', 1)
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

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (186, '缓存删除', 'monitor:cache:delete', 3, 1, 185, '', '', '', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    status = VALUES(status);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 185),
(1, 186);

SELECT '[OK] add7.sql finished' AS result;
