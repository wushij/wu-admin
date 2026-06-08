-- =============================================================================
-- add9_wuadmin.sql  生产/服务器增量补丁 #9（可重复执行）
-- =============================================================================
-- 群成员：免打扰（仅 @ 提醒）、群公告已读时间
-- 库名 wuadmin，与 application-prod.yml 一致。
-- 适用：已用 admin_platform_mysql56.sql 装过的旧库，或尚未合并 add9 的生产库。
--
-- 用法: mysql -u wuadmin -p wuadmin < sql/add9_wuadmin.sql
-- 亦可: mysql -u wuadmin -p wuadmin < sql/add6_7_wuadmin.sql（含 add6～9 合并）
-- =============================================================================

USE `wuadmin`;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_chat_group_member'
      AND COLUMN_NAME = 'notify_muted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_chat_group_member ADD COLUMN notify_muted TINYINT DEFAULT 0 COMMENT ''0正常 1免打扰仅@提醒'' AFTER muted',
    'SELECT ''notify_muted exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_chat_group_member'
      AND COLUMN_NAME = 'announcement_read_time'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_chat_group_member ADD COLUMN announcement_read_time DATETIME NULL COMMENT ''群公告已读时间'' AFTER notify_muted',
    'SELECT ''announcement_read_time exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '[OK] add9_wuadmin.sql finished' AS result;
