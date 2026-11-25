-- =============================================================================
-- add23 · AI 运维与知识库累积升级（合并 add23 ~ add29，幂等可执行）
-- -----------------------------------------------------------------------------
-- 说明：本文件由 add23 ~ add29 合并而成，按顺序覆盖：
--   add23 下线「AI 对话日志」菜单与按钮权限（保留 sys_ai_chat_log 表）
--   add24 第三方配置补齐 Tavily 与联网搜索节点
--   add25 sys_ai_chat_log 增补 attachments_json / attachment_context
--   add26 AI 模型向量参数、知识库三层表、菜单权限、旧知识数据迁移
--   add27 sys_ai_chat_log 增补 citations_json
--   add28 sys_ai_chat_log 增补 reasoning / thinking_duration_ms
--   add29 清理默认知识库迁移描述（仅匹配迁移文案）
-- 适用：MySQL 5.7+ / 8.0，全部幂等，可重复执行。
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- 源文件：add23_remove_ai_log_menu.sql
-- =============================================================================
-- =============================================================================
-- 下线「AI 对话日志」管理模块菜单与按钮权限（幂等可执行）
-- 说明：仅清理后台侧栏菜单与角色授权（id=205/206/208, path='/ai/log'），
--      保留 sys_ai_chat_log 数据表以维持 AI wu 助手自身历史会话恢复能力。
-- =============================================================================

DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT id FROM (
        SELECT id FROM sys_menu WHERE id IN (205, 206, 208) OR path = '/ai/log'
    ) AS tmp_ai_log_menus
);

DELETE FROM sys_menu
WHERE id IN (205, 206, 208) OR path = '/ai/log';

UPDATE sys_menu SET sort = 2 WHERE id = 220 AND parent_id = 210;


-- =============================================================================
-- 源文件：add24_third_party_web_search.sql
-- =============================================================================
-- =============================================================================
-- 为第三方配置 (thirdParty) 增量补齐 AI 联网搜索与 Tavily 配置字段（幂等可执行）
-- 说明：仅在缺少 $.tavily / $.webSearch 节点时按默认结构补齐，不覆盖现有第三方登录配置。
-- =============================================================================

UPDATE sys_config_group
SET config_value = JSON_SET(
        config_value,
        '$.tavily',
        JSON_OBJECT(
                'enabled', false,
                'apiKey', '',
                'searchDepth', 'basic',
                'maxResults', 5,
                'topic', 'general',
                'includeAnswer', true
        )
    ),
    remark = '微信/支付宝/GitHub/Google 第三方登录及联网搜索配置'
WHERE group_code = 'thirdParty'
  AND JSON_EXTRACT(config_value, '$.tavily') IS NULL;

UPDATE sys_config_group
SET config_value = JSON_SET(
        config_value,
        '$.webSearch',
        JSON_OBJECT(
                'enabled', true,
                'strategy', 'custom_first',
                'fallbackToDuckDuckGo', true,
                'customProviders', JSON_ARRAY()
        )
    )
WHERE group_code = 'thirdParty'
  AND JSON_EXTRACT(config_value, '$.webSearch') IS NULL;


-- =============================================================================
-- 源文件：add25_ai_chat_attachment.sql
-- =============================================================================
-- =============================================================================
-- add25 · AI wu 助手对话附件能力
-- -----------------------------------------------------------------------------
-- sys_ai_chat_log 增补两列：
--   attachments_json   本轮用户附件元数据（名称/大小/类型/下载地址，供历史回放展示）
--   attachment_context 本轮附件正文快照（供历史会话续聊时回放上下文）
-- 说明：图片二进制不入库，仅保存文件访问地址；正文快照已按单附件上限截断。
-- 适用：MySQL 5.7+ / 8.0，可重复执行。
-- =============================================================================

SET @attachments_json_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_ai_chat_log'
      AND column_name = 'attachments_json'
);

SET @ddl := IF(
    @attachments_json_exists = 0,
    'ALTER TABLE `sys_ai_chat_log` ADD COLUMN `attachments_json` MEDIUMTEXT DEFAULT NULL COMMENT ''用户附件元数据(JSON数组)'' AFTER `answer`',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @attachment_context_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_ai_chat_log'
      AND column_name = 'attachment_context'
);

SET @ddl := IF(
    @attachment_context_exists = 0,
    'ALTER TABLE `sys_ai_chat_log` ADD COLUMN `attachment_context` MEDIUMTEXT DEFAULT NULL COMMENT ''附件正文快照(用于历史上下文回放)'' AFTER `attachments_json`',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 校验查询
SELECT column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_ai_chat_log'
  AND column_name IN ('attachments_json', 'attachment_context');


-- =============================================================================
-- 源文件：add26_ai_ops_knowledge_rag.sql
-- =============================================================================
-- =============================================================================
-- add26 · AI 运维知识库 RAG（向量切片）升级
-- -----------------------------------------------------------------------------
-- 目标：
--   1. 「AI 管理」目录更名为「AI 运维」；
--   2. sys_ai_model 增加模型类型（chat / embedding）与向量维度、批量大小，
--      is_default 语义由「全局唯一」调整为「同类型内唯一」；
--   3. 新增知识库三层结构：知识库 / 文档 / 切片（含向量 JSON 副本）；
--   4. 旧扁平知识库 sys_ai_knowledge 数据迁移为「默认知识库」下的文档，
--      旧表保留只读，切片与向量由后台「重建索引」生成。
--
-- 执行说明：
--   1. 可重复执行：字段/表/菜单均已存在时自动跳过；
--   2. 旧数据迁移带幂等条件（按 legacy_knowledge_id 去重），不会重复插入；
--   3. 当前库缺少旧表时脚本只跳过对应步骤，不抛模糊错误；
--   4. Windows 命令行客户端执行时请加 --default-character-set=utf8mb4，
--      否则中文比较会报 Illegal mix of collations (gbk_chinese_ci)。
-- =============================================================================

SET @db_name := DATABASE();

-- -----------------------------------------------------------------------------
-- ① sys_ai_model：模型类型与向量参数
-- -----------------------------------------------------------------------------

SET @table_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model'
);

SET @sql := IF(
    @table_exists = 0 OR EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model' AND COLUMN_NAME = 'model_type'
    ),
    'SELECT 1',
    'ALTER TABLE sys_ai_model ADD COLUMN model_type VARCHAR(16) NOT NULL DEFAULT ''chat'' COMMENT ''模型类型: chat=对话模型 embedding=向量模型'' AFTER protocol'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @table_exists = 0 OR EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model' AND COLUMN_NAME = 'dimension'
    ),
    'SELECT 1',
    'ALTER TABLE sys_ai_model ADD COLUMN dimension INT NOT NULL DEFAULT 0 COMMENT ''向量维度（向量模型必填，0=未声明）'' AFTER max_tokens'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @table_exists = 0 OR EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model' AND COLUMN_NAME = 'batch_size'
    ),
    'SELECT 1',
    'ALTER TABLE sys_ai_model ADD COLUMN batch_size INT NOT NULL DEFAULT 16 COMMENT ''向量化批量大小（单次请求文本条数）'' AFTER dimension'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_model' AND INDEX_NAME = 'idx_type_default'
);
SET @sql := IF(
    @table_exists = 0 OR @idx_exists > 0,
    'SELECT 1',
    'ALTER TABLE sys_ai_model ADD INDEX idx_type_default (model_type, is_default)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量模型全部视为对话模型；默认模型仍保持唯一
UPDATE sys_ai_model SET model_type = 'chat' WHERE model_type IS NULL OR model_type = '';

-- -----------------------------------------------------------------------------
-- ② 知识库表：知识库 / 文档 / 切片
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `sys_ai_knowledge_base` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识库ID',
    `name` VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '知识库描述',
    `embedding_model_id` BIGINT NOT NULL DEFAULT 0 COMMENT '向量模型ID（0=跟随默认向量模型）',
    `chunk_size` INT NOT NULL DEFAULT 800 COMMENT '切片目标字符数',
    `chunk_overlap` INT NOT NULL DEFAULT 128 COMMENT '切片重叠字符数',
    `document_count` INT NOT NULL DEFAULT 0 COMMENT '文档数（冗余统计）',
    `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '切片数（冗余统计）',
    `index_status` VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT '索引状态: IDLE/INDEXING/INDEXED/FAILED',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序权重（大者优先）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_status_sort` (`status`, `sort`),
    INDEX `idx_index_status` (`index_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库（RAG）';

CREATE TABLE IF NOT EXISTS `sys_ai_knowledge_document` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文档ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `legacy_knowledge_id` BIGINT NOT NULL DEFAULT 0 COMMENT '迁移来源：旧 sys_ai_knowledge.id，0=非迁移',
    `file_id` BIGINT NOT NULL DEFAULT 0 COMMENT '原始文件ID（sys_file，迁移数据为0）',
    `file_name` VARCHAR(256) NOT NULL COMMENT '文件名',
    `file_type` VARCHAR(32) NOT NULL DEFAULT 'md' COMMENT '文件类型: md/txt/pdf/docx',
    `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `content_hash` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '正文哈希（去重）',
    `content` MEDIUMTEXT COMMENT '解析后的正文（Markdown/纯文本）',
    `char_count` INT NOT NULL DEFAULT 0 COMMENT '正文字符数',
    `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '切片数',
    `parse_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态: PENDING/PARSING/SUCCESS/FAILED',
    `index_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '向量状态: PENDING/INDEXING/INDEXED/PARTIAL/FAILED',
    `error_msg` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最近一次处理失败原因',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_kb_id` (`knowledge_base_id`, `id`),
    INDEX `idx_legacy_knowledge_id` (`legacy_knowledge_id`),
    INDEX `idx_content_hash` (`content_hash`),
    INDEX `idx_index_status` (`index_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库文档';

CREATE TABLE IF NOT EXISTS `sys_ai_knowledge_chunk` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '切片ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `document_id` BIGINT NOT NULL COMMENT '所属文档ID',
    `chunk_index` INT NOT NULL DEFAULT 0 COMMENT '文档内切片序号（从0开始）',
    `heading` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '切片所属标题',
    `content` TEXT NOT NULL COMMENT '切片正文',
    `char_count` INT NOT NULL DEFAULT 0 COMMENT '字符数',
    `token_estimate` INT NOT NULL DEFAULT 0 COMMENT 'Token 估算值',
    `embed_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '向量状态: PENDING/INDEXED/FAILED',
    `embedding_model` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '向量化使用的模型名',
    `embedding_dim` INT NOT NULL DEFAULT 0 COMMENT '向量维度',
    `embedding_vector` MEDIUMTEXT COMMENT '向量 JSON 数组（进程内索引恢复用）',
    `error_msg` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '向量化失败原因',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_doc_chunk` (`document_id`, `chunk_index`),
    INDEX `idx_kb_embed` (`knowledge_base_id`, `embed_status`),
    INDEX `idx_embed_status_id` (`embed_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库切片';

-- -----------------------------------------------------------------------------
-- ③ 菜单：「AI 管理」→「AI 运维」，补充知识库按钮权限
-- -----------------------------------------------------------------------------

UPDATE `sys_menu` SET `name` = 'AI 运维' WHERE `id` = 210;
UPDATE `sys_menu` SET `sort` = 8 WHERE `id` = 210;

INSERT INTO `sys_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`)
SELECT 225, '知识文档上传', 'system:ai-knowledge:upload', 3, 5, 220, '', '', '', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 225);

INSERT INTO `sys_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`)
SELECT 226, '知识向量重建', 'system:ai-knowledge:reindex', 3, 6, 220, '', '', '', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 226);

INSERT INTO `sys_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`)
SELECT 227, '知识检索测试', 'system:ai-knowledge:retrieve', 3, 7, 220, '', '', '', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 227);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 225), (1, 226), (1, 227),
(2, 225), (2, 226), (2, 227);

-- -----------------------------------------------------------------------------
-- ④ 旧扁平知识库迁移为「默认知识库」下的文档（幂等）
-- -----------------------------------------------------------------------------

SET @legacy_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_ai_knowledge'
);

SET @sql := IF(
    @legacy_exists = 0,
    'SELECT 1',
    'INSERT INTO `sys_ai_knowledge_base` (`name`, `chunk_size`, `chunk_overlap`, `index_status`, `status`, `sort`)
     SELECT ''默认知识库'', 800, 128, ''IDLE'', 1, 0
     FROM DUAL
     WHERE NOT EXISTS (SELECT 1 FROM `sys_ai_knowledge_base` WHERE `name` = ''默认知识库'' AND `deleted` = 0)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @legacy_exists = 0,
    'SELECT 1',
    'INSERT INTO `sys_ai_knowledge_document`
        (`knowledge_base_id`, `legacy_knowledge_id`, `file_id`, `file_name`, `file_type`, `file_size`,
         `content_hash`, `content`, `char_count`, `chunk_count`, `parse_status`, `index_status`, `status`, `creator`, `updater`)
     SELECT kb.`id`, k.`id`, 0, CONCAT(k.`title`, ''.md''), ''md'', 0,
            '''', k.`content`, CHAR_LENGTH(k.`content`), 0, ''SUCCESS'', ''PENDING'',
            IF(k.`status` = 1, 1, 0), ''migration'', ''migration''
     FROM `sys_ai_knowledge` k
     JOIN `sys_ai_knowledge_base` kb ON kb.`name` = ''默认知识库'' AND kb.`deleted` = 0
     WHERE k.`deleted` = 0
       AND NOT EXISTS (SELECT 1 FROM `sys_ai_knowledge_document` d WHERE d.`legacy_knowledge_id` = k.`id`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @legacy_exists = 0,
    'SELECT 1',
    'UPDATE `sys_ai_knowledge_base` kb
        SET kb.`document_count` = (
            SELECT COUNT(*) FROM `sys_ai_knowledge_document` d
            WHERE d.`knowledge_base_id` = kb.`id` AND d.`deleted` = 0
        )
      WHERE kb.`name` = ''默认知识库'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- ⑤ 校验
-- -----------------------------------------------------------------------------

SELECT
    (SELECT COUNT(*) FROM sys_ai_knowledge_base WHERE deleted = 0) AS knowledge_base_count,
    (SELECT COUNT(*) FROM sys_ai_knowledge_document WHERE deleted = 0) AS document_count,
    (SELECT COUNT(*) FROM sys_ai_knowledge_chunk WHERE deleted = 0) AS chunk_count,
    (SELECT COUNT(*) FROM sys_ai_model WHERE model_type = 'embedding') AS embedding_model_count;

SELECT `id`, `name`, `permission`, `type`, `parent_id`, `path`
FROM `sys_menu`
WHERE `id` IN (210, 220, 225, 226, 227);


-- =============================================================================
-- 源文件：add27_ai_chat_citations.sql
-- =============================================================================
-- =============================================================================
-- add27 · AI wu 助手联网检索参考来源持久化
-- -----------------------------------------------------------------------------
-- sys_ai_chat_log 增补一列：
--   citations_json  本轮联网搜索参考来源列表(JSON数组)，用于退出重进/历史会话回放
-- 适用：MySQL 5.7+ / 8.0，可重复执行。
-- =============================================================================

SET @citations_json_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_ai_chat_log'
      AND column_name = 'citations_json'
);

SET @ddl := IF(
    @citations_json_exists = 0,
    'ALTER TABLE `sys_ai_chat_log` ADD COLUMN `citations_json` MEDIUMTEXT DEFAULT NULL COMMENT ''联网检索参考来源(JSON数组)'' AFTER `attachment_context`',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 校验查询
SELECT column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_ai_chat_log'
  AND column_name = 'citations_json';


-- =============================================================================
-- 源文件：add28_ai_chat_reasoning.sql
-- =============================================================================
-- =============================================================================
-- add28 · AI wu 助手深度思考过程与耗时持久化
-- -----------------------------------------------------------------------------
-- sys_ai_chat_log 增补两列：
--   reasoning             推理模型深度思考正文，用于退出重进/历史会话回放
--   thinking_duration_ms  深度思考耗时（毫秒）
-- 适用：MySQL 5.7+ / 8.0，可重复执行。
-- =============================================================================

SET @reasoning_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_ai_chat_log'
      AND column_name = 'reasoning'
);

SET @ddl_reasoning := IF(
    @reasoning_exists = 0,
    'ALTER TABLE `sys_ai_chat_log` ADD COLUMN `reasoning` MEDIUMTEXT DEFAULT NULL COMMENT ''深度思考过程正文'' AFTER `answer`',
    'SELECT 1'
);

PREPARE stmt_reasoning FROM @ddl_reasoning;
EXECUTE stmt_reasoning;
DEALLOCATE PREPARE stmt_reasoning;

SET @thinking_duration_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_ai_chat_log'
      AND column_name = 'thinking_duration_ms'
);

SET @ddl_thinking_duration := IF(
    @thinking_duration_exists = 0,
    'ALTER TABLE `sys_ai_chat_log` ADD COLUMN `thinking_duration_ms` BIGINT DEFAULT NULL COMMENT ''深度思考耗时(毫秒)'' AFTER `reasoning`',
    'SELECT 1'
);

PREPARE stmt_thinking_duration FROM @ddl_thinking_duration;
EXECUTE stmt_thinking_duration;
DEALLOCATE PREPARE stmt_thinking_duration;

-- 校验查询
SELECT column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_ai_chat_log'
  AND column_name IN ('reasoning', 'thinking_duration_ms');


-- =============================================================================
-- 源文件：add29_fix_default_kb_description.sql
-- =============================================================================
-- =============================================================================
-- add29 · 清理「默认知识库」迁移描述（幂等可执行）
-- -----------------------------------------------------------------------------
-- 背景：add26 与 server_upgrade_23_to_29 在迁移旧版知识库时，会写入一句内部迁移
--       说明作为 description，移动端知识库详情页会把它当作副标题展示，属于噪声。
-- 处理：仅当描述仍为迁移脚本写入的已知文案时置空，不覆盖用户自行编辑的描述；
--       前端对空描述不渲染副标题，用户后续可自行填写真实描述。
-- 适用：MySQL 5.7+ / 8.0，可重复执行。
-- =============================================================================

UPDATE `sys_ai_knowledge_base`
SET `description` = ''
WHERE `name` = '默认知识库'
  AND `description` IN (
      '由旧版 AI 知识库自动迁移，可在「重建索引」后启用向量检索',
      '旧版知识库迁移数据，重建索引后启用向量检索'
  );

-- 校验查询
SELECT `id`, `name`, `description`, `index_status`
FROM `sys_ai_knowledge_base`
WHERE `name` = '默认知识库' AND `deleted` = 0;

