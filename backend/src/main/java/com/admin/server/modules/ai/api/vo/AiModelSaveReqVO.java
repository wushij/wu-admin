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
    /** 接入协议: openai / anthropic，留空默认 openai */
    private String protocol;
    private String modelName;
    private String baseUrl;
    /** 自定义请求路径模板，支持 {model} 占位；为空按协议内置规则拼接 */
    private String pathTemplate;
    /** 额外请求头 JSON，如 {"HTTP-Referer":"https://example.com"} */
    private String extraHeaders;
    /** 明文 API Key，服务端 SM4 加密后入库；编辑时留空则不修改 */
    private String apiKey;
    private Double temperature;
    private Integer maxTokens;
    private String systemPrompt;
    private Integer isDefault;
    private Integer status;
    private String remark;
}
