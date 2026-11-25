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
    /** 模型类型: chat=对话模型（默认）/ embedding=向量模型 */
    private String modelType;
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
    /** 向量维度（向量模型必填） */
    private Integer dimension;
    /** 向量化批量大小（向量模型可选） */
    private Integer batchSize;
    private String systemPrompt;
    private Integer isDefault;
    private Integer status;
    private String remark;
}
