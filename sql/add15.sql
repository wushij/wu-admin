-- =============================================================================
-- add15.sql  本地增量补丁 #15（可重复执行）
-- =============================================================================
-- 部门表增加负责人用户 ID，并按负责人姓名回填关联
-- 用法: mysql -u root -p wu-admin < sql/add15.sql
-- 执行后请重启后端服务。
-- =============================================================================

USE `wu-admin`;
SET NAMES utf8mb4;

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

SELECT '[OK] add15.sql finished (#15 dept leader_user_id)' AS result;
