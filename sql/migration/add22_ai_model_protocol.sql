-- =============================================================================
-- wu-admin · AI 模型接入协议扩展（add22）
-- -----------------------------------------------------------------------------
-- 目标：
--   让「自定义」供应商可以按协议接入不同厂商的对话模型，而不再只支持
--   OpenAI Chat Completions 一种协议。本脚本只增加字段和调整默认值，不改动
--   其他业务数据。
--
-- 新增字段：
--   protocol       接入协议，openai（OpenAI 兼容，默认）/ anthropic（Anthropic Messages）
--   path_template  自定义请求路径模板，支持 {model} 占位；
--                  留空时按协议内置规则拼接（openai: /chat/completions，
--                  anthropic: /v1/messages）
--   extra_headers  额外请求头 JSON，例如 {"HTTP-Referer":"https://example.com"}，
--                  用于 OpenRouter 这类需要来源标识的网关
--
-- 默认值调整：
--   max_tokens     单次回复输出上限默认值 4096 -> 8192，并同步仍为 4096 的存量配置
--
-- 执行说明：
--   1. 本脚本可以重复执行：字段已存在会自动跳过，不会报 Duplicate column。
--   2. 脚本在当前数据库执行，不会自动切换库。执行前可用 SELECT DATABASE(); 确认。
--   3. 当前库没有 sys_ai_model 表时，脚本会输出明确提示，不会抛模糊错误。
--   4. 存量记录的 protocol 会自动取默认值 openai，行为与升级前保持一致。
-- =============================================================================

SET @db_name := DATABASE();
SET @table_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model'
);

-- -----------------------------------------------------------------------------
-- ① 单次回复输出上限默认值 4096 -> 8192
-- -----------------------------------------------------------------------------
-- 原因：
--   推理类模型（deepseek-reasoner、o 系列等）的思维链同样占用输出预算，
--   4096 容易出现「思考完没有正文」或回答被截断，默认值统一提升到 8192。
-- -----------------------------------------------------------------------------

SET @sql := IF(@table_exists = 0,
    'SELECT ''当前数据库没有 sys_ai_model 表，请切换到 wu-admin 或 wuadmin 后重新执行'' AS message',
    'ALTER TABLE sys_ai_model MODIFY COLUMN max_tokens INT DEFAULT 8192 COMMENT ''单次回复最大 Token 数'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@table_exists = 0,
    'SELECT 1',
    'UPDATE sys_ai_model SET max_tokens = 8192 WHERE max_tokens = 4096'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- ② 接入协议字段扩展（字段已存在则跳过）
-- -----------------------------------------------------------------------------

SET @sql := IF(
    @table_exists = 0 OR EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model' AND COLUMN_NAME = 'protocol'
    ),
    'SELECT 1',
    'ALTER TABLE sys_ai_model ADD COLUMN protocol VARCHAR(32) NOT NULL DEFAULT ''openai'' COMMENT ''接入协议: openai（OpenAI 兼容）/anthropic（Anthropic Messages）'' AFTER provider'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @table_exists = 0 OR EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model' AND COLUMN_NAME = 'path_template'
    ),
    'SELECT 1',
    'ALTER TABLE sys_ai_model ADD COLUMN path_template VARCHAR(500) DEFAULT '''' COMMENT ''自定义请求路径模板，支持 {model} 占位，空则按协议内置规则'' AFTER base_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @table_exists = 0 OR EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model' AND COLUMN_NAME = 'extra_headers'
    ),
    'SELECT 1',
    'ALTER TABLE sys_ai_model ADD COLUMN extra_headers VARCHAR(1000) DEFAULT '''' COMMENT ''额外请求头 JSON，如网关来源标识'' AFTER path_template'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT @db_name AS executed_database, @table_exists AS table_exists;
