package com.admin.server.modules.ai.api.vo;

import lombok.Data;

/**
 * AI 模型连通性测试入参
 * 传 id 则用已保存配置测试（apiKey 留空取库中密钥）；不传 id 则按表单临时配置测试
 */
@Data
public class AiModelTestReqVO {
    private Long id;
    private String provider;
    /** 接入协议: openai / anthropic，留空默认 openai */
    private String protocol;
    private String modelName;
    private String baseUrl;
    /** 自定义请求路径模板，支持 {model} 占位 */
    private String pathTemplate;
    /** 额外请求头 JSON */
    private String extraHeaders;
    /** 明文 API Key；id 存在且此处留空时取库中已保存密钥 */
    private String apiKey;
}
