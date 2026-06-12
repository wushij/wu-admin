-- =============================================================================
-- add17.sql  本地增量补丁 #17（可重复执行）
-- =============================================================================
-- 调度日志支持软删除，清空后可在回收中心恢复
-- 用法: mysql -u root -p wu-admin < sql/add17.sql
-- 执行后请重启后端服务。
-- =============================================================================

USE `wu-admin`;
SET NAMES utf8mb4;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_log'
      AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_job_log ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT ''是否删除'' AFTER duration_ms',
    'SELECT ''deleted exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_log'
      AND COLUMN_NAME = 'create_time'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_job_log ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER deleted',
    'SELECT ''create_time exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_log'
      AND COLUMN_NAME = 'update_time'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_job_log ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time',
    'SELECT ''update_time exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_job_log
SET update_time = COALESCE(stop_time, start_time, NOW())
WHERE update_time IS NULL;

SELECT '[OK] add17.sql finished (#17 job log soft delete)' AS result;
