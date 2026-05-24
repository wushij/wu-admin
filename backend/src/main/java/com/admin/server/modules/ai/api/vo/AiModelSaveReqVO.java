package com.admin.server.modules.ai.api.vo;

import lombok.Data;

/**
 * AI 模型配置新增/编辑入参
 * 编辑时 apiKey 传空表示保持原密钥不变
 */
@Data
public class AiModelSaveReqVO {
    private Long id;
    private String name;
    private String provider;
    private String modelName;
    private String baseUrl;
    /** 明文 API Key，服务端 SM4 加密后入库；编辑时留空则不修改 */
    private String apiKey;
    private Double temperature;
    private Integer maxTokens;
    private String systemPrompt;
    private Integer isDefault;
    private Integer status;
    private String remark;
}
