package com.admin.server.modules.ai.service.search.vo;

import lombok.Data;

/**
 * 自定义联网搜索引擎配置项（对应 thirdParty.webSearch.customProviders 元素）
 */
@Data
public class CustomWebSearchEngineConfig {

    /** 引擎唯一标识 */
    private String id;

    /** 引擎显示名称，如：DeepSeek 联网搜索 */
    private String name;

    /** 预设类型标识：deepseek / bocha / serper / searxng / tavily_compat / custom_http */
    private String preset;

    /** 接入协议：openai_search（兼容大模型联网接口）或 http_json（通用 JSON HTTP 搜索接口） */
    private String protocol;

    /** 是否启用 */
    private Boolean enabled;

    /** 优先级（升序执行，数字越小越优先） */
    private Integer priority;

    /** 接口请求地址（如 https://api.deepseek.com/chat/completions 或检索 API URL） */
    private String endpointUrl;

    /** HTTP 请求方法（POST / GET，http_json 协议使用） */
    private String httpMethod;

    /** 鉴权 API Key（数据库明文或前端脱敏提交时保留旧值） */
    private String apiKey;

    /** 鉴权请求头名（默认 Authorization） */
    private String authHeader;

    /** 鉴权值前缀（默认 "Bearer "，可为空或 "Token " 等） */
    private String authPrefix;

    /** 模型名称（openai_search 协议使用，如 deepseek-chat） */
    private String modelName;

    /** OpenAI 兼容端点附加 JSON 参数（如 {"enable_search":true}） */
    private String extraBodyJson;

    /** http_json 协议请求体或 URL 参数模板，支持 {{query}} 与 {{limit}} 占位符 */
    private String requestTemplate;

    /** JSON 响应中结果数组路径（支持点号，如 results / organic / data.webPages.value） */
    private String itemsPath;

    /** 结果项标题字段路径 */
    private String titleField;

    /** 结果项链接字段路径 */
    private String urlField;

    /** 结果项摘要字段路径 */
    private String snippetField;

    /** 单次请求超时时间（毫秒，默认 10000，范围 2000~30000） */
    private Integer timeoutMs;

    /** 备注说明 */
    private String remark;
}
