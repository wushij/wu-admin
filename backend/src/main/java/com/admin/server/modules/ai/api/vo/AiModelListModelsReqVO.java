package com.admin.server.modules.ai.api.vo;

import lombok.Data;

/**
 * 拉取供应商可用模型列表入参
 * <p>传 id 时以库中配置为底（apiKey 留空则复用已保存密钥），
 * 表单里修改过的字段会覆盖库中值；不传 id 则完全按表单临时配置探测。</p>
 */
@Data
public class AiModelListModelsReqVO {
    private Long id;
    private String provider;
    /** 接入协议: openai / anthropic，留空默认 openai */
    private String protocol;
    private String baseUrl;
    /** 自定义请求路径模板，支持 {model} 占位 */
    private String pathTemplate;
    /** 额外请求头 JSON */
    private String extraHeaders;
    /** 明文 API Key；id 存在且此处留空时取库中已保存密钥 */
    private String apiKey;
}
