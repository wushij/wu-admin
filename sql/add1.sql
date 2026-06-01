-- =============================================================================
-- add1.sql  增量补丁 #1（可重复执行，无 DROP）
-- =============================================================================
-- 仅含本版本新增项，不是全量升级脚本。
--
-- 用法:
--   mysql -u root -p wu-admin < sql/add1.sql
--
-- 说明:
--   · 极旧库缺表/缺菜单 → 先执行 admin_platform.sql 文末「附录」（约 910 行起）
--   · 日常发版增量 → 依次执行 add1.sql、add2.sql …
--   · 空库安装 → 直接执行 admin_platform.sql 全文
-- =============================================================================

USE `wu-admin`;

-- [add1] 清理旧版附录曾建的冗余单列索引（已存在复合索引替代）
DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;

DELIMITER $$

CREATE PROCEDURE sp_drop_index_if_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
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

DELIMITER ;

CALL sp_drop_index_if_exists('sys_notice', 'idx_user_read_status');
CALL sp_drop_index_if_exists('sys_sms_log', 'idx_phone');

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;

SELECT '[OK] add1.sql finished' AS result;
