-- =============================================================================
-- add13.sql  本地增量补丁 #13（可重复执行，MySQL 8+）
-- =============================================================================
-- gen_table 软删除 + 回收中心支持；唯一索引改为 (table_name, deleted)
-- 用法: mysql -u root -p wu-admin < sql/add13.sql
-- =============================================================================

USE `wu-admin`;
SET NAMES utf8mb4;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gen_table'
      AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE gen_table ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''是否删除'' AFTER update_time',
    'SELECT ''deleted exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE gen_table SET deleted = 0 WHERE deleted IS NULL;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists$$
CREATE PROCEDURE sp_drop_index_if_exists(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;
    IF v_cnt > 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[DROP] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

DROP PROCEDURE IF EXISTS sp_add_unique_index_if_not_exists$$
CREATE PROCEDURE sp_add_unique_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;
    IF v_cnt = 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE INDEX `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[ADD] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

DELIMITER ;

CALL sp_drop_index_if_exists('gen_table', 'idx_gen_table_name');
CALL sp_drop_index_if_exists('gen_table', 'uk_gen_table_name');
CALL sp_add_unique_index_if_not_exists('gen_table', 'uk_gen_table_name_deleted', 'table_name, deleted');

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;
DROP PROCEDURE IF EXISTS sp_add_unique_index_if_not_exists;

SELECT '[OK] add13.sql finished (#13 gen recycle)' AS result;
