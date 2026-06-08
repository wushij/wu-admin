-- =============================================================================
-- add14_wuadmin.sql  生产/服务器增量补丁 #14（可重复执行）
-- =============================================================================
-- 系统监控下「API访问统计」排序调至最底（sort=6）
-- 用法: mysql -u wuadmin -p wuadmin < sql/add14_wuadmin.sql
-- =============================================================================

USE `wuadmin`;
SET NAMES utf8mb4;

UPDATE sys_menu SET sort = 6 WHERE id = 101 AND parent_id = 100;

SELECT '[OK] add14_wuadmin.sql finished (#14 api-access menu sort)' AS result;
