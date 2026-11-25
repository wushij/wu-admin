package com.admin.server.modules.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 对话日志表字段启动期幂等自检
 * <p>
 * 确保旧库升级后 {@code sys_ai_chat_log.citations_json}、{@code reasoning}、
 * {@code thinking_duration_ms} 自动存在，避免未手动执行迁移脚本时导致历史会话保存或回放异常。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AiChatSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumnExists(
                "citations_json",
                "ALTER TABLE `sys_ai_chat_log` ADD COLUMN `citations_json` MEDIUMTEXT DEFAULT NULL "
                        + "COMMENT '联网检索参考来源(JSON数组)' AFTER `attachment_context`",
                "sql/migration/add27_ai_chat_citations.sql");
        ensureColumnExists(
                "reasoning",
                "ALTER TABLE `sys_ai_chat_log` ADD COLUMN `reasoning` MEDIUMTEXT DEFAULT NULL "
                        + "COMMENT '深度思考过程正文' AFTER `answer`",
                "sql/migration/add28_ai_chat_reasoning.sql");
        ensureColumnExists(
                "thinking_duration_ms",
                "ALTER TABLE `sys_ai_chat_log` ADD COLUMN `thinking_duration_ms` BIGINT DEFAULT NULL "
                        + "COMMENT '深度思考耗时(毫秒)' AFTER `reasoning`",
                "sql/migration/add28_ai_chat_reasoning.sql");
    }

    private void ensureColumnExists(String columnName, String alterSql, String scriptHint) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_log' AND column_name = ?",
                    Integer.class,
                    columnName);
            if (count != null && count == 0) {
                jdbcTemplate.execute(alterSql);
                log.info("已自动为 sys_ai_chat_log 补齐 {} 字段", columnName);
            }
        } catch (Exception e) {
            log.warn("启动自检 sys_ai_chat_log.{} 失败，请手动执行 {}: {}", columnName, scriptHint, e.getMessage());
        }
    }
}
