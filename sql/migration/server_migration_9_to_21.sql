-- =============================================================================
-- wu-admin · 服务器生产环境增量合并迁移脚本（从版本 9 升级至版本 21）
-- -----------------------------------------------------------------------------
-- 【执行说明 · 必读】
--   1. 本脚本专为服务器环境设计，一键合并执行 9 到 21 之间所有的增量更新：
--      - add9  · AI 菜单与知识库权限绑定
--      - add10 · 满血反爬与安全风控审计表（sys_security_event）及管理菜单
--      - add11 · 标准组织（3部门）与黄金岗位（3岗位）精简、文件菜单恢复与旧租户菜单清理
--      - add12 · 文件存储表（sys_file）字段容量加固
--      - add13 · 应用层滑动窗口限流参数补齐（rateLimit 配置组）
--      - add14 · 风险分封禁阈值配置化（security 配置组）
--      - add16 · 现代科技感菜单图标对齐与系统管理菜单规范排序
--      - add17 · AI 知识库全面升级为 Markdown 文档知识库与 Reading 图标美化
--      - add18 · 彻底删除租户隔离机制（删列/删索引/删表），启用组织部门数据权限
--      - add19 · 旧库基线表结构补齐（补齐 18 张核心业务表与配置组默认数据）
--      - add20 · 文件存储反查高效复合索引（idx_file_path_bucket）
--      - add21 · 安全拦截事件统计与筛选复合索引（idx_action_time）
--
--   2. 【数据库名称提示 · 执行前务必确认】
--      - 后端生产环境（application-prod.yml）默认数据库名为：`wuadmin`
--      - 开发环境默认数据库名为：`wu-admin`
--      ★ 脚本第 47~48 行的 CREATE DATABASE / USE 语句默认使用 `wuadmin`。
--        若服务器上的实际数据库名称为 `wu-admin` 或其他名称，请务必先将这两行的 `wuadmin`
--        替换为实际库名。库名不一致时脚本会在新建的空库上“执行成功”，
--        但生产库不会被修改——这是生产环境迁移中最容易踩的坑。
--
--   3. 【执行方式】
--      - Linux 命令行执行（推荐）：
--        mysql -u wuadmin -p -D wuadmin < server_migration_9_to_21.sql
--      - Navicat / DBeaver 图形界面执行：
--        先在左侧双击打开目标数据库，使其处于绿色连接状态，右键选择「运行 SQL 文件」。
--
--   4. 【幂等性与数据风险】
--      建表、加列、加索引、改配置、删租户列均支持重复执行；
--      但“组织与岗位精简”属于清空重建：会删除 sys_dept / sys_post / sys_user_post
--      中的现有数据，并重建为 3 个标准部门与 3 个标准岗位，同时把所有用户部门
--      统一归到研发部。若线上已有真实组织架构或岗位数据，执行前必须备份！
--      生产环境执行前，强烈建议先对原数据库做一次快照/备份！
--
--   5. 【执行顺序】
--      脚本已按 add19 建表 → add10/add12/add20 结构 → add13/add14 配置
--      → add18 删租户列 → add11 组织岗位 → add9/add10/add16/add17 菜单 的顺序编排，
--      其中 add18 已前置到 add11 之前，避免旧库 tenant_id NOT NULL 导致岗位/部门插入失败。
-- =============================================================================

-- 自动创建（若不存在）并切换至目标数据库
CREATE DATABASE IF NOT EXISTS `wuadmin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wuadmin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;


-- =============================================================================
-- 第一部分：旧库基线表结构补齐（源自 add19，CREATE TABLE IF NOT EXISTS，幂等建表）
-- =============================================================================

-- 1. AI 知识库表
CREATE TABLE IF NOT EXISTS `sys_ai_knowledge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识ID',
    `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '知识标题',
    `keywords` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '检索关键词(逗号分隔)',
    `content` TEXT NOT NULL COMMENT '知识正文(Markdown)',
    `category` VARCHAR(32) NOT NULL DEFAULT 'faq' COMMENT '分类: faq/manual/module/other',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序权重(大者优先)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_status_category` (`status`, `category`),
    FULLTEXT INDEX `ft_knowledge` (`title`, `keywords`, `content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库';

-- 2. 定时任务表
CREATE TABLE IF NOT EXISTS `sys_job` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `job_name` VARCHAR(64) NOT NULL COMMENT '任务名称',
    `job_group` VARCHAR(64) DEFAULT 'DEFAULT' COMMENT '任务组名',
    `invoke_target` VARCHAR(500) NOT NULL COMMENT '调用目标字符串',
    `cron_expression` VARCHAR(255) DEFAULT NULL COMMENT 'cron执行表达式',
    `misfire_policy` TINYINT DEFAULT 3 COMMENT '计划执行错误策略(1-立即执行 2-执行一次 3-放弃执行)',
    `concurrent` TINYINT DEFAULT 1 COMMENT '是否并发执行(0-允许 1-禁止)',
    `status` TINYINT DEFAULT 0 COMMENT '状态(0-暂停 1-正常)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_job_group` (`job_group`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表';

-- 3. 定时任务日志表
CREATE TABLE IF NOT EXISTS `sys_job_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `job_name` VARCHAR(64) NOT NULL COMMENT '任务名称',
    `job_group` VARCHAR(64) DEFAULT NULL COMMENT '任务组名',
    `invoke_target` VARCHAR(500) DEFAULT NULL COMMENT '调用目标字符串',
    `job_message` VARCHAR(500) DEFAULT NULL COMMENT '日志信息',
    `status` TINYINT DEFAULT 0 COMMENT '执行状态(0-正常 1-失败)',
    `exception_info` VARCHAR(2000) DEFAULT NULL COMMENT '异常信息',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `stop_time` DATETIME DEFAULT NULL COMMENT '停止时间',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_job_name` (`job_name`),
    INDEX `idx_start_time` (`start_time`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务日志表';

-- 4. 系统通知/公告
CREATE TABLE IF NOT EXISTS `sys_announce` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `notice_type` TINYINT DEFAULT 1 COMMENT '1通知 2公告',
    `channels` VARCHAR(200) DEFAULT '["station"]' COMMENT '推送渠道JSON',
    `target_type` TINYINT DEFAULT 3 COMMENT '1指定用户 2按部门 3全部',
    `target_ids` VARCHAR(500) DEFAULT NULL COMMENT '目标ID JSON数组',
    `status` TINYINT DEFAULT 0 COMMENT '0草稿 1已发布',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建人昵称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `creator` VARCHAR(64) DEFAULT '',
    `updater` VARCHAR(64) DEFAULT '',
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知/公告';

-- 5. 用户通知已读记录
CREATE TABLE IF NOT EXISTS `sys_user_announce` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '接收用户',
    `announce_id` BIGINT NOT NULL COMMENT '通知ID',
    `is_read` TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    `read_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_announce` (`user_id`, `announce_id`),
    INDEX `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知已读';

-- 6. 通知推送日志
CREATE TABLE IF NOT EXISTS `sys_announce_send_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `announce_id` BIGINT NOT NULL,
    `channel` VARCHAR(50) NOT NULL COMMENT 'station等',
    `status` TINYINT DEFAULT 1 COMMENT '0失败 1成功',
    `target_count` INT DEFAULT 0,
    `success_count` INT DEFAULT 0,
    `error_msg` VARCHAR(500) DEFAULT NULL,
    `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_announce_id` (`announce_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知推送日志';

-- 7. 私聊消息
CREATE TABLE IF NOT EXISTS `sys_chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` BIGINT NOT NULL,
    `sender_name` VARCHAR(50) DEFAULT NULL,
    `sender_avatar` VARCHAR(255) DEFAULT NULL,
    `receiver_id` BIGINT NOT NULL,
    `content` TEXT,
    `msg_type` TINYINT DEFAULT 1 COMMENT '1文本 2图片',
    `is_read` TINYINT DEFAULT 0,
    `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_sender` (`sender_id`),
    INDEX `idx_receiver` (`receiver_id`),
    INDEX `idx_receiver_unread` (`receiver_id`, `is_read`, `sender_id`),
    INDEX `idx_pair_time` (`sender_id`, `receiver_id`, `send_time`),
    INDEX `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息';

-- 8. 聊天黑名单
CREATE TABLE IF NOT EXISTS `sys_user_blacklist` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '拉黑方',
    `blocked_user_id` BIGINT NOT NULL COMMENT '被拉黑用户',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_blocked` (`user_id`, `blocked_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天黑名单';

-- 9. 群聊表
CREATE TABLE IF NOT EXISTS `sys_chat_group` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `owner_id` BIGINT NOT NULL,
    `announcement` VARCHAR(500) DEFAULT NULL,
    `max_members` INT DEFAULT 200,
    `status` TINYINT DEFAULT 1 COMMENT '0解散 1正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊';

-- 10. 群成员表
CREATE TABLE IF NOT EXISTS `sys_chat_group_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `nickname` VARCHAR(50) DEFAULT NULL,
    `role` TINYINT DEFAULT 0 COMMENT '0成员 1管理员 2群主',
    `muted` TINYINT DEFAULT 0,
    `notify_muted` TINYINT DEFAULT 0 COMMENT '0正常 1免打扰仅@提醒',
    `announcement_read_time` DATETIME DEFAULT NULL COMMENT '群公告已读时间',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员';

-- 11. 群消息表
CREATE TABLE IF NOT EXISTS `sys_chat_group_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `sender_name` VARCHAR(50) DEFAULT NULL,
    `sender_avatar` VARCHAR(500) DEFAULT NULL,
    `content` TEXT NOT NULL,
    `msg_type` TINYINT DEFAULT 1,
    `mention_ids` VARCHAR(500) DEFAULT NULL COMMENT '@的用户ID列表JSON',
    `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_group_time` (`group_id`, `send_time`),
    INDEX `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群消息';

-- 12. 群聊操作日志
CREATE TABLE IF NOT EXISTS `sys_chat_group_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL COMMENT '群ID',
    `action_type` VARCHAR(32) NOT NULL COMMENT '操作类型',
    `operator_id` BIGINT NOT NULL COMMENT '操作人',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人昵称',
    `target_user_id` BIGINT DEFAULT NULL COMMENT '目标用户',
    `target_user_name` VARCHAR(50) DEFAULT NULL COMMENT '目标用户昵称',
    `detail` VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_group_time` (`group_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊操作日志';

-- 13. 短信发送记录表
CREATE TABLE IF NOT EXISTS `sys_sms_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '短信内容/验证码',
    `sms_type` VARCHAR(20) DEFAULT 'verify_code' COMMENT 'verify_code / notice / marketing',
    `template_id` VARCHAR(50) DEFAULT NULL COMMENT '模板ID',
    `template_params` VARCHAR(500) DEFAULT NULL COMMENT '模板参数 JSON',
    `provider` VARCHAR(20) DEFAULT NULL COMMENT 'aliyun / tencent / console',
    `status` TINYINT DEFAULT 0 COMMENT '0-发送中 1-成功 2-失败',
    `result_msg` VARCHAR(500) DEFAULT NULL COMMENT '结果信息',
    `biz_id` VARCHAR(100) DEFAULT NULL COMMENT '服务商消息ID',
    `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `biz_type` VARCHAR(30) DEFAULT NULL COMMENT '业务类型',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_phone_create_time` (`phone`, `create_time`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_send_time` (`send_time`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信发送记录表';

-- 14. 系统配置分组表
CREATE TABLE IF NOT EXISTS `sys_config_group` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `group_code` VARCHAR(50) NOT NULL COMMENT '分组编码 login/register/security等',
    `group_name` VARCHAR(100) NOT NULL COMMENT '分组名称',
    `config_value` TEXT NOT NULL COMMENT 'JSON 配置',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `creator` VARCHAR(64) DEFAULT '',
    `updater` VARCHAR(64) DEFAULT '',
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_group_code` (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置分组';

-- 15. 代码生成业务表
CREATE TABLE IF NOT EXISTS `gen_table` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    `table_name` VARCHAR(200) DEFAULT '' COMMENT '表名称',
    `table_comment` VARCHAR(500) DEFAULT '' COMMENT '表描述',
    `class_name` VARCHAR(100) DEFAULT '' COMMENT '实体类名称',
    `package_name` VARCHAR(100) DEFAULT '' COMMENT '生成包路径',
    `module_name` VARCHAR(30) DEFAULT '' COMMENT '生成模块名',
    `business_name` VARCHAR(30) DEFAULT '' COMMENT '生成业务名',
    `function_name` VARCHAR(50) DEFAULT '' COMMENT '生成功能名',
    `author` VARCHAR(50) DEFAULT '' COMMENT '作者',
    `gen_type` VARCHAR(10) DEFAULT 'crud' COMMENT '生成类型',
    `gen_path` VARCHAR(200) DEFAULT '/' COMMENT '生成路径',
    `front_type` VARCHAR(30) DEFAULT 'element-plus' COMMENT '前端模板',
    `form_layout` VARCHAR(20) DEFAULT 'vertical' COMMENT '表单布局',
    `parent_menu_id` BIGINT DEFAULT NULL COMMENT '上级菜单ID',
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_gen_table_name_deleted` (`table_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表';

-- 16. 代码生成字段表
CREATE TABLE IF NOT EXISTS `gen_table_column` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `table_id` BIGINT DEFAULT NULL,
    `column_name` VARCHAR(200) DEFAULT '',
    `column_comment` VARCHAR(500) DEFAULT '',
    `column_type` VARCHAR(100) DEFAULT '',
    `java_type` VARCHAR(50) DEFAULT '',
    `java_field` VARCHAR(200) DEFAULT '',
    `is_pk` TINYINT DEFAULT 0,
    `is_increment` TINYINT DEFAULT 0,
    `is_required` TINYINT DEFAULT 0,
    `is_insert` TINYINT DEFAULT 0,
    `is_edit` TINYINT DEFAULT 0,
    `is_list` TINYINT DEFAULT 0,
    `is_query` TINYINT DEFAULT 0,
    `query_type` VARCHAR(20) DEFAULT 'EQ',
    `html_type` VARCHAR(50) DEFAULT '',
    `dict_type` VARCHAR(100) DEFAULT '',
    `sort` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_gen_col_table` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成字段表';

-- 17. AI 模型供应商配置表
CREATE TABLE IF NOT EXISTS `sys_ai_model` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模型配置ID',
    `name` VARCHAR(100) NOT NULL COMMENT '配置名称（展示用，如「DeepSeek 官方」）',
    `provider` VARCHAR(32) NOT NULL COMMENT '供应商: deepseek/openai/qwen/kimi',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称（如 deepseek-chat / qwen-plus）',
    `base_url` VARCHAR(255) NOT NULL COMMENT 'API 基础地址（不含 /chat/completions）',
    `api_key` VARCHAR(1024) DEFAULT '' COMMENT 'API 密钥（SM4-CBC 加密存储）',
    `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '采样温度 0~2',
    `max_tokens` INT DEFAULT 4096 COMMENT '单次回复最大 Token 数',
    `system_prompt` VARCHAR(2000) DEFAULT NULL COMMENT '系统提示词（角色设定）',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模型 0:否 1:是（全局唯一）',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_provider` (`provider`),
    INDEX `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型供应商配置表';

-- 18. AI 对话日志审计表
CREATE TABLE IF NOT EXISTS `sys_ai_chat_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `user_id` BIGINT DEFAULT 0 COMMENT '提问用户ID',
    `username` VARCHAR(64) DEFAULT '' COMMENT '提问用户名',
    `conversation_id` VARCHAR(64) DEFAULT '' COMMENT '会话ID（前端生成，串联多轮对话）',
    `model_id` BIGINT DEFAULT 0 COMMENT '模型配置ID',
    `provider` VARCHAR(32) DEFAULT '' COMMENT '供应商',
    `model_name` VARCHAR(100) DEFAULT '' COMMENT '模型名称',
    `question` TEXT COMMENT '用户提问（脱敏后）',
    `answer` MEDIUMTEXT COMMENT 'AI 回答',
    `prompt_tokens` INT DEFAULT 0 COMMENT '提问 Token 消耗',
    `completion_tokens` INT DEFAULT 0 COMMENT '回答 Token 消耗',
    `total_tokens` INT DEFAULT 0 COMMENT '总 Token 消耗',
    `duration_ms` BIGINT DEFAULT 0 COMMENT '耗时（毫秒）',
    `chat_status` TINYINT DEFAULT 1 COMMENT '结果 1:成功 0:失败 2:用户中断',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `source` VARCHAR(20) DEFAULT 'pc' COMMENT '来源终端 pc/mobile',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_model_id` (`model_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话日志审计表';

-- 18.1 幂等补齐 sys_ai_chat_log.tool_trace（对齐基线 add8 的 L3 Function Calling 工具轨迹）
--      9 版本库通常已有该列；仅当表缺失后由本脚本重建时才会真正补列。
SET @has_tool_trace := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_ai_chat_log'
      AND column_name = 'tool_trace'
);
SET @ddl_tool_trace := IF(@has_tool_trace = 0,
    'ALTER TABLE `sys_ai_chat_log` ADD COLUMN `tool_trace` TEXT DEFAULT NULL COMMENT ''工具调用轨迹(JSON数组)'' AFTER `error_msg`',
    'SELECT 1');
PREPARE stmt_tool_trace FROM @ddl_tool_trace;
EXECUTE stmt_tool_trace;
DEALLOCATE PREPARE stmt_tool_trace;

-- 19. 安全补齐 12 个系统配置分组默认基线数据（INSERT IGNORE 保证不覆盖已有配置）
INSERT IGNORE INTO `sys_config_group` (`group_code`, `group_name`, `config_value`, `remark`) VALUES
('site', '基础信息', '{"platformName":"Admin Platform","platformSubtitle":"统一运维 · 高效管控","loginWelcome":"Welcome","registerTitle":"Sign Up","copyright":"","icpEnabled":true,"icpNumber":"粤ICP备XXXXXXXX号-1","icpUrl":"https://beian.miit.gov.cn"}', '平台展示名称与登录页文案'),
('session', '会话配置', '{"tokenExpireHours":24,"sessionSignExpireHours":24}', 'JWT 与 Redis 会话有效期（小时）'),
('file', '文件配置', '{"maxSizeMb":50,"allowedExtensions":"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov"}', '文件管理上传限制'),
('rateLimit', '接口限流', '{"captchaPerIpMinute":40,"loginPerIpMinute":30,"registerPerIpMinute":10,"smsPerIpMinute":5,"smsSendIntervalSeconds":60,"smsPerPhoneDaily":10,"smsPerIpDaily":30,"apiPerIpMinute":240,"authPerIpMinute":60,"filesPerIpMinute":600}', '认证接口按 IP 限流；含应用层滑动窗口与短信防刷'),
('login', '登录配置', '{"captchaEnabled":true,"captchaType":"image","smsLoginEnabled":false,"smsLoginSliderCaptchaEnabled":false,"rememberMe":true,"maxRetryCount":5,"maxRetryCountIp":20,"lockTime":10}', '验证码 image/slider；smsLoginEnabled 短信登录；smsLoginSliderCaptchaEnabled 短信发送前滑块；maxRetryCount 账号锁定阈值；maxRetryCountIp IP 锁定阈值'),
('register', '注册配置', '{"enabled":true,"captchaEnabled":true,"captchaType":"image","defaultRoleCode":"user","needAudit":false,"minPasswordLength":6}', '开放注册、验证码类型、默认角色、是否审核'),
('thirdParty', '第三方配置', '{"wechat":{"enabled":false,"appId":"","appSecret":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":""},"github":{"enabled":false,"clientId":"","clientSecret":""},"google":{"enabled":false,"clientId":"","clientSecret":"","redirectUri":""}}', '微信/支付宝/GitHub/Google 第三方登录'),
('payment', '支付配置', '{"wechatPay":{"enabled":false,"mchId":"","appId":"","apiV3Key":"","privateKey":"","certSerialNo":"","notifyUrl":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":"","signType":"RSA2","gatewayUrl":"https://openapi.alipay.com/gateway.do","notifyUrl":"","returnUrl":""}}', '微信/支付宝支付与测试下单'),
('sms', '短信配置', '{"enabled":false,"provider":"aliyunAuth","accessKeyId":"","accessKeySecret":"","signName":"","tencentAppId":"","templateVerifyCode":"100001","templateModifyPhone":"100002","templateResetPassword":"100003","templateBindPhone":"100004","templateVerifyBindPhone":"100005","schemeName":"","codeExpireMinutes":5}', '阿里云短信认证/腾讯云'),
('email', '邮件配置', '{"enabled":false,"provider":"qq","host":"smtp.qq.com","port":465,"username":"","password":"","fromName":"wu-admin 系统团队","authEnabled":true,"securityType":"SSL","connectionTimeoutMs":5000,"timeoutMs":5000,"writeTimeoutMs":5000,"encoding":"UTF-8","debug":false,"codeExpireMinutes":5,"codeLength":6,"dailyLimitPerEmail":20,"sendIntervalSeconds":60}', '企业级 SMTP 邮件服务配置：发件人、SSL端口、超时限额与验证码防刷规则'),
('security', '安全配置', '{"disableDevtool":false,"isConcurrent":false,"sm4EncryptEnabled":true,"sm2SignEnabled":true,"sm3SignEnabled":true,"timestampEnabled":true,"nonceEnabled":true,"riskBlockScore":80,"riskBanScore":120}', '安全防线与会话：SM4加密（会话密钥）、HMAC-SM3数字签名、时间戳与Nonce防重放、风险分封禁阈值'),
('ai', 'AI助手配置', '{"assistantEnabled":true,"globalKnowledge":"","answerScope":"focus","tokensPerUserDaily":100000,"roleTokenQuotas":[]}', 'AI 助手配置：回答边界（focus/open）、每用户每日 Token 配额与角色配额');

-- 20. 默认 AI 知识库示范数据（INSERT IGNORE）
INSERT IGNORE INTO `sys_ai_knowledge` (`id`, `title`, `keywords`, `content`, `category`, `sort`, `status`, `creator`, `updater`) VALUES
(1, '忘记密码如何找回', '忘记密码,找回密码,重置密码,登录不了',
 '## 忘记密码找回流程\n1. 打开登录页，点击「忘记密码」。\n2. 输入账号绑定的邮箱，获取验证码。\n3. 输入验证码后设置新密码即可重新登录。\n\n> 若账号未绑定邮箱，请联系管理员在「系统管理 → 用户管理」中重置密码。',
 'faq', 10, 1, 'admin', 'admin'),
(2, '如何提交与跟进工单', '工单,提交工单,报障,问题反馈',
 '## 工单提交流程\n1. 进入「系统管理 → 工单管理」，点击「新建工单」。\n2. 填写标题、问题描述与优先级后提交。\n3. 处理进度可在工单列表查看，处理完成后可进行确认或评价。',
 'manual', 5, 1, 'admin', 'admin');


-- =============================================================================
-- 第二部分：反爬与安全风控拦截事件表及复合索引（源自 add10 & add21）
-- =============================================================================

CREATE TABLE IF NOT EXISTS `sys_security_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型: UA_BLOCK/ANTI_REPLAY/SIGN_FAIL/TIMESTAMP_INVALID/RATE_LIMIT/RISK_BLOCK',
  `principal` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '主体标识(IP或账号)',
  `ipaddr` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端真实IP',
  `request_uri` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '请求路径',
  `request_method` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '请求方法(GET/POST/PUT/DELETE)',
  `user_agent` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '请求User-Agent头',
  `risk_delta` INT NOT NULL DEFAULT 0 COMMENT '本次触发风险分增量',
  `total_risk_score` INT NOT NULL DEFAULT 0 COMMENT '触发后累计风险分',
  `action` VARCHAR(32) NOT NULL DEFAULT 'BLOCKED' COMMENT '处置动作: BLOCKED/FORBIDDEN/WARNING',
  `reason` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '触发原因详细说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_ipaddr` (`ipaddr`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_action_time` (`action`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全审计与反爬拦截事件表';

-- 幂等补充 add21 针对存量 sys_security_event 表的安全拦截事件统计索引 idx_action_time
SET @index_sec_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_security_event'
      AND index_name = 'idx_action_time'
);

SET @ddl_sec := IF(
    @index_sec_exists = 0,
    'ALTER TABLE `sys_security_event` ADD INDEX `idx_action_time` (`action`, `create_time`)',
    'SELECT 1'
);

PREPARE stmt_idx_sec FROM @ddl_sec;
EXECUTE stmt_idx_sec;
DEALLOCATE PREPARE stmt_idx_sec;


-- =============================================================================
-- 第三部分：文件存储字段容量加固与高效复合索引（源自 add12 & add20）
-- =============================================================================

-- 1. 字段容量加固（对齐短哈希+安全文件名长度要求）
-- 2. 幂等创建文件归属反查索引 idx_file_path_bucket
-- 两者统一放进存储过程：sys_file 表不存在时自动跳过，避免脚本中途中断
DROP PROCEDURE IF EXISTS sp_harden_sys_file;
DELIMITER $$
CREATE PROCEDURE sp_harden_sys_file()
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE index_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'sys_file';

    IF table_exists > 0 THEN
        ALTER TABLE `sys_file`
            MODIFY COLUMN `original_name` VARCHAR(255) DEFAULT '' COMMENT '原始文件名',
            MODIFY COLUMN `file_name` VARCHAR(300) NOT NULL COMMENT '存储文件名',
            MODIFY COLUMN `url` VARCHAR(1024) DEFAULT '' COMMENT '访问URL',
            MODIFY COLUMN `file_suffix` VARCHAR(32) DEFAULT '' COMMENT '后缀';

        SELECT COUNT(*) INTO index_exists
        FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'sys_file'
          AND index_name = 'idx_file_path_bucket';

        IF index_exists = 0 THEN
            ALTER TABLE `sys_file` ADD INDEX `idx_file_path_bucket` (`file_path`, `bucket_name`);
        END IF;
    END IF;
END$$
DELIMITER ;

CALL sp_harden_sys_file();
DROP PROCEDURE IF EXISTS sp_harden_sys_file;


-- =============================================================================
-- 第四部分：限流与安全风险分参数补齐（源自 add13 & add14）
-- =============================================================================

-- 1. 应用层滑动窗口限流参数补齐（apiPerIpMinute / authPerIpMinute / filesPerIpMinute）
UPDATE `sys_config_group`
SET `config_value` = JSON_SET(
    `config_value`,
    '$.apiPerIpMinute', 240,
    '$.authPerIpMinute', 60,
    '$.filesPerIpMinute', 600
)
WHERE `group_code` = 'rateLimit'
  AND JSON_VALID(`config_value`);

-- 2. 风险分封禁阈值参数补齐（riskBlockScore / riskBanScore）
UPDATE `sys_config_group`
SET `config_value` = JSON_SET(
    `config_value`,
    '$.riskBlockScore', 80,
    '$.riskBanScore', 120
)
WHERE `group_code` = 'security'
  AND JSON_VALID(`config_value`);


-- =============================================================================
-- 第五部分：彻底删除租户隔离机制并启用组织部门数据权限（源自 add18）
-- =============================================================================

-- 1. 幂等删除包含 tenant_id 的旧租户联合唯一索引
DROP PROCEDURE IF EXISTS sp_drop_tenant_unique_index;
DELIMITER $$
CREATE PROCEDURE sp_drop_tenant_unique_index()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'sys_post'
                 AND index_name = 'uk_tenant_post_code') THEN
        ALTER TABLE `sys_post` DROP INDEX `uk_tenant_post_code`;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'sys_role'
                 AND index_name = 'uk_tenant_code') THEN
        ALTER TABLE `sys_role` DROP INDEX `uk_tenant_code`;
    END IF;
END$$
DELIMITER ;

CALL sp_drop_tenant_unique_index();
DROP PROCEDURE IF EXISTS sp_drop_tenant_unique_index;

-- 2. 恢复岗位编码、角色编码的全局唯一索引（无重复数据时安全建立）
DROP PROCEDURE IF EXISTS sp_restore_global_unique_index;
DELIMITER $$
CREATE PROCEDURE sp_restore_global_unique_index()
BEGIN
    DECLARE duplicate_count INT DEFAULT 0;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = 'sys_post') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                       WHERE table_schema = DATABASE() AND table_name = 'sys_post'
                         AND index_name = 'uk_post_code') THEN
            SELECT COUNT(*) INTO duplicate_count FROM (
                SELECT `post_code` FROM `sys_post`
                WHERE `deleted` = 0
                GROUP BY `post_code` HAVING COUNT(*) > 1
            ) AS dup;
            IF duplicate_count = 0 THEN
                ALTER TABLE `sys_post` ADD UNIQUE KEY `uk_post_code` (`post_code`);
            END IF;
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = 'sys_role') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                       WHERE table_schema = DATABASE() AND table_name = 'sys_role'
                         AND index_name = 'uk_code') THEN
            SELECT COUNT(*) INTO duplicate_count FROM (
                SELECT `code` FROM `sys_role`
                WHERE `deleted` = 0
                GROUP BY `code` HAVING COUNT(*) > 1
            ) AS dup;
            IF duplicate_count = 0 THEN
                ALTER TABLE `sys_role` ADD UNIQUE KEY `uk_code` (`code`);
            END IF;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL sp_restore_global_unique_index();
DROP PROCEDURE IF EXISTS sp_restore_global_unique_index;

-- 3. 幂等删除全库业务表的 idx_tenant_id 索引与 tenant_id 列
DROP PROCEDURE IF EXISTS sp_drop_tenant_column_if_exists;
DELIMITER $$
CREATE PROCEDURE sp_drop_tenant_column_if_exists(IN target_table VARCHAR(64))
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE index_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = target_table;

    IF table_exists > 0 THEN
        SELECT COUNT(*) INTO index_exists
        FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = target_table
          AND index_name = 'idx_tenant_id';

        IF index_exists > 0 THEN
            SET @ddl = CONCAT('ALTER TABLE `', target_table, '` DROP INDEX `idx_tenant_id`');
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        SELECT COUNT(*) INTO column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = target_table
          AND column_name = 'tenant_id';

        IF column_exists > 0 THEN
            SET @ddl = CONCAT('ALTER TABLE `', target_table, '` DROP COLUMN `tenant_id`');
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL sp_drop_tenant_column_if_exists('sys_user');
CALL sp_drop_tenant_column_if_exists('sys_role');
CALL sp_drop_tenant_column_if_exists('sys_user_role');
CALL sp_drop_tenant_column_if_exists('sys_user_post');
CALL sp_drop_tenant_column_if_exists('sys_role_menu');
CALL sp_drop_tenant_column_if_exists('sys_dept');
CALL sp_drop_tenant_column_if_exists('sys_post');
CALL sp_drop_tenant_column_if_exists('sys_ticket');
CALL sp_drop_tenant_column_if_exists('sys_ticket_comment');
CALL sp_drop_tenant_column_if_exists('sys_ticket_attachment');
CALL sp_drop_tenant_column_if_exists('sys_approval_form');
CALL sp_drop_tenant_column_if_exists('sys_approval_record');
CALL sp_drop_tenant_column_if_exists('sys_announce');
CALL sp_drop_tenant_column_if_exists('sys_user_announce');
CALL sp_drop_tenant_column_if_exists('sys_announce_send_log');
CALL sp_drop_tenant_column_if_exists('sys_notice');
CALL sp_drop_tenant_column_if_exists('sys_file');
CALL sp_drop_tenant_column_if_exists('sys_file_group');
CALL sp_drop_tenant_column_if_exists('sys_chat_message');
CALL sp_drop_tenant_column_if_exists('sys_chat_group');
CALL sp_drop_tenant_column_if_exists('sys_chat_group_member');
CALL sp_drop_tenant_column_if_exists('sys_chat_group_message');
CALL sp_drop_tenant_column_if_exists('sys_chat_group_log');
CALL sp_drop_tenant_column_if_exists('sys_user_blacklist');
CALL sp_drop_tenant_column_if_exists('sys_oper_log');
CALL sp_drop_tenant_column_if_exists('sys_login_log');
CALL sp_drop_tenant_column_if_exists('sys_ai_chat_log');
CALL sp_drop_tenant_column_if_exists('sys_security_event');

DROP PROCEDURE IF EXISTS sp_drop_tenant_column_if_exists;

-- 4. 彻底删除历史租户主表
DROP TABLE IF EXISTS `sys_tenant`;

-- 5. 文件与文件分组增加 dept_id 部门归属字段并回填存量数据
DROP PROCEDURE IF EXISTS sp_add_dept_column_if_missing;
DELIMITER $$
CREATE PROCEDURE sp_add_dept_column_if_missing(IN target_table VARCHAR(64))
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE index_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = target_table;

    IF table_exists > 0 THEN
        SELECT COUNT(*) INTO column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = target_table
          AND column_name = 'dept_id';

        IF column_exists = 0 THEN
            SET @ddl = CONCAT('ALTER TABLE `', target_table,
                '` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT \'归属部门ID\' AFTER `id`');
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        SELECT COUNT(*) INTO index_exists
        FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = target_table
          AND index_name = 'idx_dept_id';

        IF index_exists = 0 THEN
            SET @ddl = CONCAT('ALTER TABLE `', target_table, '` ADD INDEX `idx_dept_id` (`dept_id`)');
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL sp_add_dept_column_if_missing('sys_file');
CALL sp_add_dept_column_if_missing('sys_file_group');

DROP PROCEDURE IF EXISTS sp_add_dept_column_if_missing;

-- 按创建人账号自动回填文件的部门归属
UPDATE `sys_file` f
JOIN `sys_user` u ON u.`username` = f.`create_by`
SET f.`dept_id` = u.`dept_id`
WHERE f.`dept_id` IS NULL;

UPDATE `sys_file_group` g
JOIN `sys_user` u ON u.`username` = g.`create_by`
SET g.`dept_id` = u.`dept_id`
WHERE g.`dept_id` IS NULL;

-- 无法按 create_by 归属的历史脏数据安全脱钩与软删除
UPDATE `sys_file` SET `group_id` = NULL
WHERE `group_id` IN (SELECT `id` FROM `sys_file_group` WHERE `dept_id` IS NULL);

DELETE FROM `sys_file_group` WHERE `dept_id` IS NULL;

UPDATE `sys_file` SET `deleted` = 1, `update_time` = NOW()
WHERE `dept_id` IS NULL AND `deleted` = 0;

-- 6. 角色数据范围字段补齐与初始化（组织隔离体系：1全部 2自定义 3本部门 4本部门及以下 5仅本人）
SET @has_data_scope = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_role' AND column_name = 'data_scope'
);
SET @ddl = IF(@has_data_scope = 0,
    'ALTER TABLE `sys_role` ADD COLUMN `data_scope` TINYINT DEFAULT 1 COMMENT \'数据范围 1:全部 2:自定义 3:本部门 4:本部门及以下 5:仅本人\' AFTER `remark`',
    'SELECT 1');
PREPARE stmt_ds FROM @ddl;
EXECUTE stmt_ds;
DEALLOCATE PREPARE stmt_ds;

SET @has_data_scope_dept_ids = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_role' AND column_name = 'data_scope_dept_ids'
);
SET @ddl = IF(@has_data_scope_dept_ids = 0,
    'ALTER TABLE `sys_role` ADD COLUMN `data_scope_dept_ids` VARCHAR(500) DEFAULT NULL COMMENT \'数据范围部门ID\' AFTER `data_scope`',
    'SELECT 1');
PREPARE stmt_ds_ids FROM @ddl;
EXECUTE stmt_ds_ids;
DEALLOCATE PREPARE stmt_ds_ids;

-- 超管默认赋予全部数据权限(1)，普通用户默认赋予本部门及以下(4)
UPDATE `sys_role` SET `data_scope` = 1
WHERE `code` = 'super_admin' AND (`data_scope` IS NULL OR `data_scope` = 0);

UPDATE `sys_role` SET `data_scope` = 4
WHERE `code` = 'user' AND (`data_scope` IS NULL OR `data_scope` = 1);

-- 7. 清理历史残留租户管理菜单及其角色关联
DELETE FROM `sys_role_menu` WHERE `menu_id` = 109 OR `menu_id` BETWEEN 240 AND 244;
DELETE FROM `sys_menu` WHERE `id` = 109 AND `permission` = 'system:tenant:list';
DELETE FROM `sys_menu` WHERE `id` BETWEEN 240 AND 244;


-- =============================================================================
-- 第六部分：标准组织与黄金岗位精简（源自 add11）
-- =============================================================================
-- ★ 数据风险提示：
--   本节会先清空 sys_dept / sys_post / sys_user_post，再重建 3 个标准部门与
--   3 个标准岗位，并把所有用户部门统一归到研发部（id=2）。
--   若线上已有真实组织架构、岗位或用户岗位关系，执行前必须备份；
--   如不需要精简组织，可整段注释本节（第五部分与第七部分不依赖本节数据）。

-- 1. 组织管理（sys_dept）精简：只保留 3 个标准部门
UPDATE `sys_user` SET `dept_id` = 2
WHERE `dept_id` NOT IN (1, 2, 3) OR `dept_id` IS NULL;
UPDATE `sys_user` SET `dept_id` = 1 WHERE `id` = 1; -- 超管归属总公司
UPDATE `sys_user` SET `dept_id` = 2 WHERE `id` = 2; -- 张三归属研发部

DELETE FROM `sys_dept`;
INSERT INTO `sys_dept` (`id`, `name`, `parent_id`, `ancestors`, `sort`, `status`, `leader_name`, `leader_user_id`, `deleted`)
VALUES
(1, '总公司', 0, '0', 1, 1, '管理员', 1, 0),
(2, '研发部', 1, '0,1', 1, 1, '张三', 2, 0),
(3, '运营部', 1, '0,1', 2, 1, NULL, NULL, 0);

ALTER TABLE `sys_dept` AUTO_INCREMENT = 4;

-- 2. 岗位管理（sys_post）精简：只保留 3 个黄金岗位
DELETE FROM `sys_user_post`;
DELETE FROM `sys_post`;

INSERT INTO `sys_post` (`id`, `parent_id`, `post_code`, `post_name`, `sort`, `status`, `remark`, `deleted`)
VALUES
(1, 0, 'ceo', '总经理', 1, 1, '企业最高经营管理负责人', 0),
(2, 1, 'cto', '技术总监', 2, 1, '技术与研发整体负责人', 0),
(3, 2, 'dev', '研发工程师', 3, 1, '核心开发与技术实现', 0);

ALTER TABLE `sys_post` AUTO_INCREMENT = 4;

-- 绑定核心用户岗位：admin 绑定总经理(id=1)，zhangsan 绑定研发工程师(id=3)
INSERT INTO `sys_user_post` (`user_id`, `post_id`) VALUES
(1, 1),
(2, 3);


-- =============================================================================
-- 第七部分：菜单体系全面升级、修复与现代图标排序对齐
-- （涵盖 add9, add10, add11, add16, add17）
-- =============================================================================

-- 1. 恢复与修复文件管理菜单（110~113），防止历史脚本误覆盖
UPDATE `sys_menu`
SET `parent_id` = 105, `name` = '文件列表', `path` = '/system/file',
    `component` = 'system/file/index', `icon` = 'DocumentCopy', `sort` = 1,
    `type` = 2, `permission` = 'sys:file:list', `status` = 1
WHERE `id` = 110 AND (`permission` = 'system:tenant:query' OR `name` LIKE '%租户%');

UPDATE `sys_menu`
SET `parent_id` = 110, `name` = '文件查询', `path` = '', `component` = '',
    `icon` = '', `sort` = 1, `type` = 3, `permission` = 'sys:file:query', `status` = 1
WHERE `id` = 111 AND (`permission` = 'system:tenant:add' OR `name` LIKE '%租户%');

UPDATE `sys_menu`
SET `parent_id` = 110, `name` = '文件上传', `path` = '', `component` = '',
    `icon` = '', `sort` = 2, `type` = 3, `permission` = 'sys:file:upload', `status` = 1
WHERE `id` = 112 AND (`permission` = 'system:tenant:edit' OR `name` LIKE '%租户%');

UPDATE `sys_menu`
SET `parent_id` = 110, `name` = '文件删除', `path` = '', `component` = '',
    `icon` = '', `sort` = 3, `type` = 3, `permission` = 'sys:file:delete', `status` = 1
WHERE `id` = 113 AND (`permission` = 'system:tenant:delete' OR `name` LIKE '%租户%');

-- 若 110~113 记录不存在则安全补齐
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort`, `type`, `permission`, `status`) VALUES
(110, 105, '文件列表', '/system/file', 'system/file/index', 'DocumentCopy', 1, 2, 'sys:file:list', 1),
(111, 110, '文件查询', '', '', '', 1, 3, 'sys:file:query', 1),
(112, 110, '文件上传', '', '', '', 2, 3, 'sys:file:upload', 1),
(113, 110, '文件删除', '', '', '', 3, 3, 'sys:file:delete', 1);

-- 2. 挂载与初始化【安全风控】菜单（id=230~235，挂在系统日志 parent_id=120 下，使用现代锁图标 Lock）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort`, `type`, `permission`, `status`, `create_time`, `update_time`)
VALUES (230, 120, '安全风控', 'system/security-log', 'system/security-log/index', 'Lock', 3, 2, 'system:security:list', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `name` = VALUES(`name`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = 'Lock',
  `status` = 1,
  `permission` = VALUES(`permission`);

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort`, `type`, `permission`, `status`, `create_time`, `update_time`) VALUES
(231, 230, '安全查询', '', '', '', 1, 3, 'system:security:query', 1, NOW(), NOW()),
(232, 230, '风险解封', '', '', '', 2, 3, 'system:security:unblock', 1, NOW(), NOW()),
(233, 230, '手动拉黑', '', '', '', 3, 3, 'system:security:block', 1, NOW(), NOW()),
(234, 230, '清空安全日志', '', '', '', 4, 3, 'system:security:clear', 1, NOW(), NOW()),
(235, 230, '删除安全日志', '', '', '', 5, 3, 'system:security:delete', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `permission` = VALUES(`permission`), `status` = 1;

-- 3. 挂载与初始化 AI 模块各级菜单与【AI 知识库】（使用图书阅读图标 Reading）
INSERT IGNORE INTO `sys_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`) VALUES
(210, 'AI 管理', '', 1, 8, 0, '/ai', 'Compass', '', 1),
(200, 'AI 模型配置', 'system:ai-model:list', 2, 1, 210, '/ai/model', 'AiModel', 'ai/model/index', 1),
(207, 'AI模型查询', 'system:ai-model:query', 3, 1, 200, '', '', '', 1),
(201, 'AI模型新增', 'system:ai-model:create', 3, 2, 200, '', '', '', 1),
(202, 'AI模型修改', 'system:ai-model:update', 3, 3, 200, '', '', '', 1),
(203, 'AI模型删除', 'system:ai-model:delete', 3, 4, 200, '', '', '', 1),
(204, 'AI模型测试', 'system:ai-model:test', 3, 5, 200, '', '', '', 1),
(205, 'AI 对话日志', 'system:ai-log:list', 2, 2, 210, '/ai/log', 'ChatDotRound', 'ai/log/index', 1),
(208, 'AI日志查询', 'system:ai-log:query', 3, 1, 205, '', '', '', 1),
(206, 'AI日志删除', 'system:ai-log:delete', 3, 2, 205, '', '', '', 1),
(220, 'AI 知识库', 'system:ai-knowledge:list', 2, 3, 210, '/ai/knowledge', 'Reading', 'ai/knowledge/index', 1),
(224, 'AI知识查询', 'system:ai-knowledge:query', 3, 1, 220, '', '', '', 1),
(221, 'AI知识新增', 'system:ai-knowledge:create', 3, 2, 220, '', '', '', 1),
(222, 'AI知识修改', 'system:ai-knowledge:update', 3, 3, 220, '', '', '', 1),
(223, 'AI知识删除', 'system:ai-knowledge:delete', 3, 4, 220, '', '', '', 1);

-- 4. 现代美观图标对齐（彻底摆脱陈旧呆板图标）
UPDATE `sys_menu` SET `icon` = 'Calendar' WHERE `id` = 120 OR `path` = '/log';                         -- 系统日志 -> 日历 Calendar
UPDATE `sys_menu` SET `icon` = 'Compass' WHERE `id` = 210 OR `path` = '/ai';                           -- AI 管理 -> 科技罗盘 Compass
UPDATE `sys_menu` SET `icon` = 'AiModel' WHERE `id` = 200 OR `path` = '/ai/model';                     -- AI 模型配置 -> 现代AI智能星芒 AiModel
UPDATE `sys_menu` SET `icon` = 'Reading' WHERE `id` = 220 OR `path` = '/ai/knowledge';                 -- AI 知识库 -> 专业图书阅读 Reading
UPDATE `sys_menu` SET `icon` = 'Odometer' WHERE `id` = 187 OR `path` = '/monitor/server';              -- 服务监控 -> 实时负载仪表盘 Odometer
DELETE FROM `sys_role_menu` WHERE `menu_id` = 129;
DELETE FROM `sys_menu` WHERE `id` = 129;
UPDATE `sys_menu` SET `icon` = 'Lock' WHERE `id` = 230 OR `path` = '/system/security-log';    -- 安全风控 -> 经典安全锁 Lock
UPDATE `sys_menu` SET `icon` = 'Workflow' WHERE `id` = 8 OR `path` = '/workflow';                      -- 流程中心 -> 工作流分支流转 Workflow

-- 5. 系统管理子菜单逻辑排序优化（重排为高频直观顺序：1用户->2角色->3菜单->4组织->5字典->6回收->7配置）
UPDATE `sys_menu` SET `sort` = 1 WHERE `id` = 2;   -- 用户管理
UPDATE `sys_menu` SET `sort` = 2 WHERE `id` = 3;   -- 角色管理
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 4;   -- 菜单管理
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 5;   -- 组织管理
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 130; -- 字典管理
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 163; -- 回收中心
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 160; -- 系统配置

-- 6. 顶级目录排序优化
UPDATE `sys_menu` SET `sort` = 8 WHERE `id` = 210; -- AI 管理
UPDATE `sys_menu` SET `sort` = 9 WHERE `id` = 150; -- 开发工具

-- 7. 角色菜单权限安全绑定与更新
-- 7.1 超级管理员（role_id=1）：授予全部新增系统能力权限（AI、知识库、安全风控、文件管理等）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 110), (1, 111), (1, 112), (1, 113),                                 -- 文件管理
(1, 210), (1, 200), (1, 207), (1, 201), (1, 202), (1, 203), (1, 204), -- AI 模型管理
(1, 205), (1, 208), (1, 206),                                           -- AI 对话日志
(1, 220), (1, 224), (1, 221), (1, 222), (1, 223),                     -- AI 知识库
(1, 230), (1, 231), (1, 232), (1, 233), (1, 234), (1, 235);             -- 安全风控

-- 7.2 普通用户角色（role_id=2）：仅授予 AI 知识库与菜单的只读查看权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 220), (2, 224);


-- =============================================================================
-- 第八部分：迁移后结果校验与健康检查
-- =============================================================================

-- 1. 检查是否存在残留的 tenant_id 字段（预期结果为 0）
SELECT COUNT(*) AS remaining_tenant_columns
FROM information_schema.columns
WHERE table_schema = DATABASE() AND column_name = 'tenant_id';

-- 2. 检查核心新增业务表是否齐全
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'sys_ai_knowledge', 'sys_ai_model', 'sys_ai_chat_log',
    'sys_security_event', 'sys_config_group', 'sys_job',
    'sys_announce', 'sys_chat_message', 'gen_table'
  )
ORDER BY table_name;

-- 3. 检查系统管理菜单排序是否规范对齐
SELECT `id`, `parent_id`, `name`, `sort`, `icon`, `path`
FROM `sys_menu`
WHERE `parent_id` = 1
ORDER BY `sort` ASC, `id` ASC;

-- 4. 检查各模块图标是否均已升级为科技感现代图标
SELECT `id`, `name`, `icon`, `path`
FROM `sys_menu`
WHERE `id` IN (8, 120, 187, 200, 210, 220, 230)
ORDER BY `id` ASC;

-- 5. 检查限流与安全风控 JSON 配置参数
SELECT `group_code`, `config_value`
FROM `sys_config_group`
WHERE `group_code` IN ('rateLimit', 'security', 'ai');

-- =============================================================================
-- 迁移完成！请重启后端服务使配置与实体缓存完全同步。
-- =============================================================================
