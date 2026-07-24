-- =============================================================================
-- add4_email_config · 企业级 SMTP 邮件服务配置增量补丁（本地开发环境 wu-admin）
-- =============================================================================
-- 说明: 为系统配置增加企业级 SMTP 邮件服务、超时限额与验证码防刷规则
-- 用法: mysql -u root -p wu-admin < sql/add4_email_config.sql
-- 可重复执行（幂等更新，已有库直接执行即可）
-- =============================================================================

USE `wu-admin`;

-- 1. 更新邮件配置 sys_config_group (若已存在 email 记录，合并新选项)
UPDATE sys_config_group
SET config_value = '{"enabled":true,"provider":"qq","host":"smtp.qq.com","port":465,"username":"974473458@qq.com","password":"cqjvfpulydqwbegh","fromName":"wu-admin 系统团队","authEnabled":true,"securityType":"SSL","connectionTimeoutMs":5000,"timeoutMs":5000,"writeTimeoutMs":5000,"encoding":"UTF-8","debug":false,"codeExpireMinutes":5,"codeLength":6,"dailyLimitPerEmail":20,"sendIntervalSeconds":60}',
    remark = '企业级 SMTP 邮件服务配置：发件人、SSL端口、超时限额与验证码防刷规则',
    update_time = NOW()
WHERE group_code = 'email';

-- 2. 若不存在 email 配置项则初始化插入
INSERT INTO sys_config_group (group_code, group_name, config_value, remark)
SELECT 'email', '邮件配置', '{"enabled":true,"provider":"qq","host":"smtp.qq.com","port":465,"username":"974473458@qq.com","password":"cqjvfpulydqwbegh","fromName":"wu-admin 系统团队","authEnabled":true,"securityType":"SSL","connectionTimeoutMs":5000,"timeoutMs":5000,"writeTimeoutMs":5000,"encoding":"UTF-8","debug":false,"codeExpireMinutes":5,"codeLength":6,"dailyLimitPerEmail":20,"sendIntervalSeconds":60}', '企业级 SMTP 邮件服务配置：发件人、SSL端口、超时限额与验证码防刷规则'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config_group WHERE group_code = 'email');

SELECT '[OK] 本地开发库 wu-admin 企业级邮件配置增量补丁执行完毕！' AS status;
