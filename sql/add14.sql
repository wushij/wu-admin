-- =============================================================================
-- add14.sql  本地增量补丁 #14（可重复执行）
-- =============================================================================
-- 系统监控下「API访问统计」排序调至最底（sort=6）
-- 用法: mysql -u root -p wu-admin < sql/add14.sql
-- =============================================================================

USE `wu-admin`;
SET NAMES utf8mb4;

UPDATE sys_menu SET sort = 6 WHERE id = 101 AND parent_id = 100;

SELECT '[OK] add14.sql finished (#14 api-access menu sort)' AS result;
