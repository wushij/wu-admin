-- =============================================================================
-- add4.sql — 性能索引增量（已有 wu-admin 库单独执行）
--
-- 用法：
--   mysql -u root -p wu-admin < sql/add4.sql
--   或在客户端中选中全文执行
--
-- 说明：
--   - 可重复执行：索引已存在则自动跳过，不再报 1061
--   - 不删除、不修改业务数据，仅 ADD INDEX
--   - 大表建索引可能耗时，建议业务低峰执行
--   - 全新空库请直接执行 admin_platform.sql
-- =============================================================================

USE `wu-admin`;

DROP PROCEDURE IF EXISTS sp_add_index_if_missing;

DELIMITER $$

CREATE PROCEDURE sp_add_index_if_missing(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;

    IF v_cnt = 0 THEN
        SET @ddl_sql = p_ddl;
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[OK] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

DELIMITER ;

-- -----------------------------------------------------------------------------
-- 1. 历史附录索引（旧库可能缺失）
-- -----------------------------------------------------------------------------
CALL sp_add_index_if_missing('sys_dict_data', 'idx_dict_type_status_deleted',
    'ALTER TABLE sys_dict_data ADD INDEX idx_dict_type_status_deleted (dict_type, status, deleted, sort)');

CALL sp_add_index_if_missing('sys_user', 'idx_deleted_status',
    'ALTER TABLE sys_user ADD INDEX idx_deleted_status (deleted, status)');

CALL sp_add_index_if_missing('sys_user_post', 'uk_user_post',
    'ALTER TABLE sys_user_post ADD UNIQUE INDEX uk_user_post (user_id, post_id)');

CALL sp_add_index_if_missing('sys_oper_log', 'idx_oper_time_status',
    'ALTER TABLE sys_oper_log ADD INDEX idx_oper_time_status (oper_time, status)');

-- -----------------------------------------------------------------------------
-- 2. 聊天 / 消息（表不存在则跳过，需先执行 add2.sql）
-- -----------------------------------------------------------------------------
CALL sp_add_index_if_missing('sys_chat_group_member', 'idx_user_id',
    'ALTER TABLE sys_chat_group_member ADD INDEX idx_user_id (user_id)');

CALL sp_add_index_if_missing('sys_chat_message', 'idx_receiver_unread',
    'ALTER TABLE sys_chat_message ADD INDEX idx_receiver_unread (receiver_id, is_read, sender_id)');

CALL sp_add_index_if_missing('sys_notice', 'idx_user_read_deleted',
    'ALTER TABLE sys_notice ADD INDEX idx_user_read_deleted (user_id, read_status, deleted)');

-- -----------------------------------------------------------------------------
-- 3. 审批 / 工单 / 文件 / API 日志
-- -----------------------------------------------------------------------------
CALL sp_add_index_if_missing('sys_approval_form', 'idx_type_applicant_status',
    'ALTER TABLE sys_approval_form ADD INDEX idx_type_applicant_status (form_type, applicant_user_id, status, deleted)');

CALL sp_add_index_if_missing('sys_ticket', 'idx_deleted_status_time',
    'ALTER TABLE sys_ticket ADD INDEX idx_deleted_status_time (deleted, status, create_time)');

CALL sp_add_index_if_missing('sys_file', 'idx_group_time',
    'ALTER TABLE sys_file ADD INDEX idx_group_time (group_id, create_time)');

CALL sp_add_index_if_missing('sys_api_access_log', 'idx_start_time_success',
    'ALTER TABLE sys_api_access_log ADD INDEX idx_start_time_success (start_time, success)');

DROP PROCEDURE IF EXISTS sp_add_index_if_missing;

-- -----------------------------------------------------------------------------
-- 4. 校验（可选）
-- -----------------------------------------------------------------------------
-- SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
-- FROM information_schema.STATISTICS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND INDEX_NAME IN (
--     'idx_dict_type_status_deleted', 'idx_deleted_status', 'uk_user_post',
--     'idx_oper_time_status', 'idx_user_id', 'idx_receiver_unread',
--     'idx_user_read_deleted', 'idx_type_applicant_status', 'idx_deleted_status_time',
--     'idx_group_time', 'idx_start_time_success'
--   )
-- GROUP BY TABLE_NAME, INDEX_NAME
-- ORDER BY TABLE_NAME, INDEX_NAME;
