-- =============================================================================
-- add3_api_security · 全链路 API 安全架构增量补丁（服务器生产环境 / 已有数据库）
-- =============================================================================
-- 说明: 为系统配置增加 SM4数据加密、SM2数字签名、时间戳校验与 Nonce防重放配置项
-- 用法: mysql -u root -p wu-admin < sql/add3_api_security.sql
-- 可重复执行（幂等更新，已有库直接执行即可）
-- =============================================================================

USE `wu-admin`;

-- 1. 更新安全配置 sys_config_group (若已存在 security 记录，安全合并新选项)
UPDATE sys_config_group
SET config_value = CASE 
    WHEN config_value LIKE '%sm4EncryptEnabled%' THEN config_value
    ELSE REPLACE(config_value, '}', ',"sm4EncryptEnabled":false,"sm2SignEnabled":false,"timestampEnabled":true,"nonceEnabled":true,"sm4SecretKey":"WuAdmin16BytesKey"}')
END,
remark = '安全防线与会话：SM4加密、SM2数字签名、时间戳与Nonce防重放'
WHERE group_code = 'security';

-- 2. 若不存在 security 配置项则初始化插入
INSERT INTO sys_config_group (group_code, group_name, config_value, remark)
SELECT 'security', '安全配置', '{"disableDevtool":false,"isConcurrent":false,"sm4EncryptEnabled":false,"sm2SignEnabled":false,"timestampEnabled":true,"nonceEnabled":true,"sm4SecretKey":"WuAdmin16BytesKey"}', '安全防线与会话：SM4加密、SM2数字签名、时间戳与Nonce防重放'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config_group WHERE group_code = 'security');

SELECT 'API 安全架构增量补丁执行完毕！' AS status;
