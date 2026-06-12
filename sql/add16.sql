-- =============================================================================
-- add16.sql  本地增量补丁 #16（可重复执行）
-- =============================================================================
-- 定时任务日志增加毫秒级耗时字段
-- 用法: mysql -u root -p wu-admin < sql/add16.sql
-- 执行后请重启后端服务。
-- =============================================================================

USE `wu-admin`;
SET NAMES utf8mb4;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_log'
      AND COLUMN_NAME = 'duration_ms'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_job_log ADD COLUMN duration_ms BIGINT DEFAULT NULL COMMENT ''执行耗时(毫秒)'' AFTER stop_time',
    'SELECT ''duration_ms exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '[OK] add16.sql finished (#16 job log duration_ms)' AS result;
