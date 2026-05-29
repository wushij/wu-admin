-- =============================================================================
-- add10.sql — 一级菜单排序调整（可重复执行）
--
-- 目标顺序：系统管理 → 业务中心 → 系统监控 → 系统日志 → 文件管理 → 消息中心 → 开发工具
-- 用法：mysql -u root -p wu-admin < sql/add10.sql
-- =============================================================================

USE `wu-admin`;

UPDATE sys_menu SET sort = 4 WHERE id = 120 AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE id = 105 AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 6 WHERE id = 170 AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 7 WHERE id = 150 AND parent_id = 0 AND deleted = 0;
