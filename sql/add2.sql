-- =============================================================================
-- add2.sql  增量补丁 #2（可重复执行，无 DROP）
-- =============================================================================
-- 仅含本版本新增项，不是全量升级脚本。
--
-- 用法:
--   mysql -u root -p wu-admin < sql/add2.sql
--
-- 说明:
--   · 极旧库缺表/缺菜单 → 先执行 admin_platform.sql 文末「附录」（约 910 行起）
--   · 日常发版增量 → 依次执行 add1.sql、add2.sql …
--   · 空库安装 → 直接执行 admin_platform.sql 全文（已含 smsLoginSliderCaptchaEnabled 默认值）
-- =============================================================================

USE `wu-admin`;

-- [add2] 登录配置：短信发送前滑块验证 smsLoginSliderCaptchaEnabled
UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.smsLoginSliderCaptchaEnabled', CAST(false AS JSON))
WHERE group_code = 'login'
  AND JSON_EXTRACT(config_value, '$.smsLoginSliderCaptchaEnabled') IS NULL;

SELECT '[OK] add2.sql finished' AS result;
