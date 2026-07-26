-- =============================================================================
-- add2 · 回收中心 query/restore/delete 权限（生产库 wuadmin）
-- =============================================================================
-- 说明: 与 sql/add2.sql 内容一致，仅目标库名为 wuadmin
-- 用法: mysql -u wuadmin -p wuadmin < sql/add2_wuadmin.sql
-- 可重复执行（INSERT IGNORE / UPDATE 幂等）
-- 执行后请重新登录并重启后端
-- =============================================================================

USE `wuadmin`;

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
