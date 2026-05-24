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
    private String modelName;
    private String baseUrl;
    /** 明文 API Key；id 存在且此处留空时取库中已保存密钥 */
    private String apiKey;
}
