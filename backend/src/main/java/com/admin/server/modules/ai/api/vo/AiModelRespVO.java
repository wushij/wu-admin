package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 模型配置出参（apiKey 仅返回掩码，绝不回传明文/密文）
 */
@Data
public class AiModelRespVO {
    private Long id;
    private String name;
    private String provider;
    private String modelName;
    private String baseUrl;
    /** API Key 掩码（如 sk-a***x9f2），未配置时为空串 */
    private String apiKeyMasked;
    /** 是否已配置 API Key（编辑时前端据此显示「留空不修改」占位） */
    private Boolean hasApiKey;
    private Double temperature;
    private Integer maxTokens;
    private String systemPrompt;
    private Integer isDefault;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
