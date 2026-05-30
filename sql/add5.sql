-- =============================================
-- 已有库增量：安全配置（前端安全 disableDevtool + 会话 isConcurrent）
-- 可重复执行，无 DROP
-- =============================================

INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('security', '安全配置', '{"disableDevtool":false,"isConcurrent":false}', '前端安全与会话：禁止调试、禁止多端同时在线')
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    remark = VALUES(remark);

-- 已有 security 分组但缺少 isConcurrent 时补默认 false（禁止多端）
UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.isConcurrent', CAST(false AS JSON))
WHERE group_code = 'security'
  AND JSON_EXTRACT(config_value, '$.isConcurrent') IS NULL;
