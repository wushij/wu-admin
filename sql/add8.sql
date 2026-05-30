-- 第三方配置补全 Google 登录字段（已有 thirdParty 分组、可重复执行）
UPDATE sys_config_group
SET config_value = JSON_SET(
  config_value,
  '$.google',
  JSON_OBJECT('enabled', false, 'clientId', '', 'clientSecret', '', 'redirectUri', '')
)
WHERE group_code = 'thirdParty'
  AND (JSON_EXTRACT(config_value, '$.google') IS NULL);
