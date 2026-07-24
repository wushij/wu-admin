-- =============================================================================
-- add3_api_security_wuadmin · 全链路 API 安全架构增量补丁（线上生产库 wuadmin）
-- =============================================================================
-- 说明: 生产环境专用库 wuadmin 补丁，增加 SM4/SM2/时间戳/Nonce 配置项
-- 用法: mysql -u wuadmin -p wuadmin < sql/add3_api_security_wuadmin.sql
-- 可重复执行（幂等更新）
-- =============================================================================

USE `wuadmin`;

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

SELECT '生产库 wuadmin API 安全架构增量补丁执行完毕！' AS status;
