-- =============================================================================
-- add1 · 在线用户查询权限（生产库 wuadmin）
-- =============================================================================
-- 说明: 与 sql/add1.sql 内容一致，仅目标库名为 wuadmin
-- 用法: mysql -u wuadmin -p wuadmin < sql/add1_wuadmin.sql
-- 可重复执行（INSERT IGNORE / UPDATE 幂等）
-- 执行后请重新登录以刷新权限缓存
-- =============================================================================

USE `wuadmin`;

-- 在线用户查询按钮 190；强退 104 排序后移
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

-- 超管默认拥有；已有「在线用户」菜单的角色同步勾选查询
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 190);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 190
FROM sys_role_menu rm
WHERE rm.menu_id = 103;
