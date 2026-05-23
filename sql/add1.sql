-- =============================================================================
-- add1.sql — 已有 wu-admin 库增量脚本（可重复执行，勿 DROP 表/库）
-- 用途：注册验证码类型、系统配置结构补全
-- 执行后请重启后端或于管理端「系统配置」点一次「保存全部」以刷新 Redis 缓存
-- =============================================================================

USE `wu-admin`;

-- -----------------------------------------------------------------------------
-- 1. 注册配置：补充 captchaType（图片 image / 滑块 slider）
-- -----------------------------------------------------------------------------
UPDATE sys_config_group
SET config_value = JSON_SET(
        COALESCE(config_value, '{}'),
        '$.captchaType',
        COALESCE(
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')), ''),
                'image'
        )
    ),
    remark = '开放注册、验证码类型、默认角色、是否审核'
WHERE group_code = 'register'
  AND (
    JSON_EXTRACT(config_value, '$.captchaType') IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')) = ''
    );

-- -----------------------------------------------------------------------------
-- 2. 登录配置：确保 captchaType 字段存在（旧库可能仅有 captchaEnabled）
-- -----------------------------------------------------------------------------
UPDATE sys_config_group
SET config_value = JSON_SET(
        COALESCE(config_value, '{}'),
        '$.captchaType',
        COALESCE(
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')), ''),
                'image'
        )
    )
WHERE group_code = 'login'
  AND (
    JSON_EXTRACT(config_value, '$.captchaType') IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaType')) = ''
    );

-- -----------------------------------------------------------------------------
-- 3. 校验（可选，执行后查看结果）
-- -----------------------------------------------------------------------------
-- SELECT group_code, config_value, remark FROM sys_config_group
-- WHERE group_code IN ('login', 'register');
