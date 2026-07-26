-- 临时关闭「禁止前端调试」（兼容 MySQL 5.6），执行后重启后端
USE `wuadmin`;

UPDATE sys_config_group
SET config_value = REPLACE(config_value, '"disableDevtool":true', '"disableDevtool":false')
WHERE group_code = 'security';
