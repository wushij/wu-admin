-- =============================================================================
-- add15_wuadmin.sql  生产/服务器增量补丁 #15+#16+#17（可重复执行）
-- =============================================================================
-- #15 部门表增加负责人用户 ID，并按负责人姓名回填关联
-- #16 定时任务日志增加毫秒级耗时字段 duration_ms
-- #17 调度日志支持软删除（deleted/create_time/update_time），清空后可进回收中心恢复
-- 用法: mysql -u wuadmin -p wuadmin < sql/add15_wuadmin.sql
-- 执行后请重启后端服务。
-- =============================================================================

USE `wuadmin`;
SET NAMES utf8mb4;

-- #15 leader_user_id
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_dept'
      AND COLUMN_NAME = 'leader_user_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_dept ADD COLUMN leader_user_id BIGINT DEFAULT NULL COMMENT ''负责人用户ID'' AFTER leader_name',
    'SELECT ''leader_user_id exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_dept d
INNER JOIN sys_user u ON u.deleted = 0
    AND (d.leader_name = u.nickname OR d.leader_name = u.username)
SET d.leader_user_id = u.id
WHERE d.leader_user_id IS NULL
  AND d.leader_name IS NOT NULL
  AND d.leader_name <> '';

UPDATE sys_dept SET leader_user_id = 1 WHERE id = 1 AND leader_user_id IS NULL AND leader_name = '管理员';
UPDATE sys_dept SET leader_user_id = 2 WHERE id = 6 AND leader_user_id IS NULL AND leader_name = '张三';

-- #16 duration_ms
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

-- #17 soft delete
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

SELECT '[OK] add15_wuadmin.sql finished (#15 leader_user_id + #16 duration_ms + #17 job log soft delete)' AS result;
