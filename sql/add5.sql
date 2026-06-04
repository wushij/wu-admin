-- =============================================================================
-- add5.sql  增量补丁 #5（可重复执行）
-- =============================================================================
-- 补全工单字典类型：sys_dict_data 里已有工单状态/优先级项，但 sys_dict_type
-- 缺少对应类型时，Redis 字典缓存与 API 会返回空，导致流转状态、优先级无选项。
-- 用法: mysql -u root -p wu-admin < sql/add5.sql
-- 生产/服务器库名 wuadmin 请用: sql/add5-prod.sql
-- 执行后请在「字典管理」点「刷新缓存」，或重启后端。
-- =============================================================================

USE `wu-admin`;

UPDATE sys_dict_type t
SET t.deleted = 0, t.status = 1
WHERE t.dict_type IN ('sys_ticket_status', 'sys_ticket_priority')
  AND t.deleted = 1;

INSERT INTO sys_dict_type (dict_name, dict_type, status, remark, deleted)
SELECT '工单状态', 'sys_ticket_status', 1, '工单流转状态', 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type t WHERE t.dict_type = 'sys_ticket_status' AND t.deleted = 0
);

INSERT INTO sys_dict_type (dict_name, dict_type, status, remark, deleted)
SELECT '工单优先级', 'sys_ticket_priority', 1, '工单优先级', 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type t WHERE t.dict_type = 'sys_ticket_priority' AND t.deleted = 0
);

SELECT '[OK] add5.sql finished' AS result;
