-- =============================================================================
-- add19_wuadmin.sql  生产/服务器增量补丁 #19（可重复执行）
-- =============================================================================
-- 菜单图标：区分文件列表 / 工单管理 / 接口文档 / 代码生成
-- 用法: mysql -u wuadmin -p wuadmin < sql/add19_wuadmin.sql
-- 执行后请重新登录以刷新侧栏菜单。
-- =============================================================================

USE `wuadmin`;
SET NAMES utf8mb4;

-- [add19] 菜单图标
UPDATE sys_menu SET icon = 'DocumentCopy' WHERE id = 110;
UPDATE sys_menu SET icon = 'Tickets' WHERE id = 7;
UPDATE sys_menu SET icon = 'Connection' WHERE id = 151;
UPDATE sys_menu SET icon = 'SetUp' WHERE id = 164;

SELECT '[OK] add19_wuadmin.sql finished (#19 menu icons)' AS result;
