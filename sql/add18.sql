-- =============================================================================
-- add18.sql  本地增量补丁 #18（可重复执行）
-- =============================================================================
-- 登录配置：账号与 IP 分开锁定阈值（maxRetryCountIp，默认 20）
-- 用法: mysql -u root -p wu-admin < sql/add18.sql
-- 执行后请重启后端服务。
-- =============================================================================

USE `wu-admin`;
SET NAMES utf8mb4;

-- [add18] 登录配置：IP 锁定阈值 maxRetryCountIp
UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.maxRetryCountIp', CAST(20 AS JSON)),
    remark = '验证码 image/slider；smsLoginEnabled 短信登录；smsLoginSliderCaptchaEnabled 短信发送前滑块；maxRetryCount 账号锁定阈值；maxRetryCountIp IP 锁定阈值'
WHERE group_code = 'login'
  AND JSON_EXTRACT(config_value, '$.maxRetryCountIp') IS NULL;

SELECT '[OK] add18.sql finished (#18 login maxRetryCountIp)' AS result;
