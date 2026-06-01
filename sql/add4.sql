-- =============================================================================
-- add4.sql  增量补丁 #4（可重复执行）
-- =============================================================================
-- 企业 IM：群消息 @ 提醒字段
-- 用法: mysql -u root -p wu-admin < sql/add4.sql
-- =============================================================================

USE `wu-admin`;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_chat_group_message'
    AND COLUMN_NAME = 'mention_ids'
);

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE sys_chat_group_message ADD COLUMN mention_ids VARCHAR(500) NULL COMMENT ''@的用户ID列表JSON'' AFTER msg_type',
  'SELECT ''mention_ids already exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '[OK] add4.sql finished' AS result;
