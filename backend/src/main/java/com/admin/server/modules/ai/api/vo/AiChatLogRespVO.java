package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话日志出参
 */
@Data
public class AiChatLogRespVO {
    private Long id;
    private Long userId;
    private String username;
    private String conversationId;
    private Long modelId;
    private String provider;
    private String modelName;
    private String question;
    private String answer;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long durationMs;
    private Integer chatStatus;
    private String errorMsg;
    private String source;
    private LocalDateTime createTime;
}
