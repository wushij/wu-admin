-- =============================================================================
-- add8.sql  增量补丁 #8（可重复执行）
-- =============================================================================
-- 系统监控下新增「服务监控」菜单（本机 JMX：CPU / 内存 / JVM / 磁盘）。
--
-- 用法: mysql -u root -p wu-admin < sql/add8.sql
-- 生产/服务器: mysql -u wuadmin -p wuadmin < sql/add6_7_wuadmin.sql
-- 执行后请重新登录。
-- =============================================================================

USE `wu-admin`;

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (187, '服务监控', 'monitor:server:list', 2, 5, 100, '/monitor/server', 'Cpu', 'monitor/server/index', 1)
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

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 187);

SELECT '[OK] add8.sql finished' AS result;
