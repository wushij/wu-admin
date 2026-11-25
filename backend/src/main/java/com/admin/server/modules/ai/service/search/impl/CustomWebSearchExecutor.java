package com.admin.server.modules.ai.service.search.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.service.search.vo.CustomWebSearchEngineConfig;
import com.admin.server.modules.ai.service.search.vo.WebSearchResult;
import com.admin.server.modules.ai.service.search.vo.WebSearchTestResultVO;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.security.WebhookUrlValidator;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义联网搜索引擎执行器
 * <p>
 * 支持两类自定义联网搜索接入模式：
 * <ul>
 *   <li>{@code openai_search}：OpenAI 兼容接口 + 自定义联网扩展体（如 DeepSeek / 火山方舟 / 百炼等带联网能力的接口）</li>
 *   <li>{@code http_json}：标准 REST HTTP 搜索接口（如博查 Bocha、Serper、SearXNG、Tavily 代理或自研搜索网关）</li>
 * </ul>
 * 所有外部地址在请求前均强制经过 {@link WebhookUrlValidator} SSRF 防护并禁用重定向。
 * </p>
 */
@Service
public class CustomWebSearchExecutor {

    private static final Logger log = LoggerFactory.getLogger(CustomWebSearchExecutor.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static final Pattern MD_LINK_PATTERN = Pattern.compile("\\[([^\\]]{2,80})]\\((https?://[^)\\s]+)\\)");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");

    @Resource
    private SystemConfigHelper systemConfigHelper;

    /** 执行单条自定义搜索引擎检索 */
    public List<WebSearchResult> execute(CustomWebSearchEngineConfig engine, String query, int limit) {
        if (engine == null || StrUtil.isBlank(query) || StrUtil.isBlank(engine.getEndpointUrl())) {
            return List.of();
        }
        String resolvedKey = resolveApiKey(engine);
        String protocol = StrUtil.blankToDefault(engine.getProtocol(), "http_json").trim().toLowerCase();
        int maxResults = limit > 0 ? Math.min(limit, 10) : 5;
        int timeoutMs = clampTimeout(engine.getTimeoutMs());

        if ("openai_search".equals(protocol)) {
            return executeOpenAiSearch(engine, resolvedKey, query.trim(), maxResults, timeoutMs);
        }
        return executeHttpJsonSearch(engine, resolvedKey, query.trim(), maxResults, timeoutMs);
    }

    /** 管理员在配置页点击「测试试搜」专用入口（保留脱敏旧密钥回填与耗时测量） */
    public WebSearchTestResultVO testEngine(CustomWebSearchEngineConfig candidate, String query) {
        if (candidate == null) {
            throw new BusinessException(400, "引擎配置不能为空");
        }
        if (StrUtil.isBlank(candidate.getEndpointUrl())) {
            throw new BusinessException(400, "接口地址不能为空");
        }
        validatePublicEndpoint(candidate.getEndpointUrl());

        String effectiveQuery = StrUtil.blankToDefault(StrUtil.trim(query), "Spring Boot 最新版本动态");
        String resolvedKey = resolveApiKey(candidate);
        CustomWebSearchEngineConfig copy = copyWithApiKey(candidate, resolvedKey);

        long start = System.currentTimeMillis();
        try {
            List<WebSearchResult> results = executeForTestStrict(copy, effectiveQuery, 3);
            long latency = System.currentTimeMillis() - start;
            if (results.isEmpty()) {
                return WebSearchTestResultVO.builder()
                        .success(false)
                        .engineName(StrUtil.blankToDefault(copy.getName(), "自定义联网引擎"))
                        .latencyMs(latency)
                        .message("接口调用已连通，但未解析出有效检索结果，请检查 JSON 路径或模型输出")
                        .results(List.of())
                        .build();
            }
            return WebSearchTestResultVO.builder()
                    .success(true)
                    .engineName(StrUtil.blankToDefault(copy.getName(), "自定义联网引擎"))
                    .latencyMs(latency)
                    .message("联网检索测试成功，共解析 " + results.size() + " 条参考结果")
                    .results(results)
                    .build();
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            return WebSearchTestResultVO.builder()
                    .success(false)
                    .engineName(StrUtil.blankToDefault(copy.getName(), "自定义联网引擎"))
                    .latencyMs(latency)
                    .message("连通性测试失败：" + ex.getMessage())
                    .results(List.of())
                    .build();
        }
    }

    private List<WebSearchResult> executeForTestStrict(CustomWebSearchEngineConfig engine, String query, int limit) throws Exception {
        String protocol = StrUtil.blankToDefault(engine.getProtocol(), "http_json").trim().toLowerCase();
        int timeoutMs = clampTimeout(engine.getTimeoutMs());
        if ("openai_search".equals(protocol)) {
            return executeOpenAiSearchStrict(engine, engine.getApiKey(), query, limit, timeoutMs);
        }
        return executeHttpJsonSearchStrict(engine, engine.getApiKey(), query, limit, timeoutMs);
    }

    private List<WebSearchResult> executeOpenAiSearch(CustomWebSearchEngineConfig engine, String apiKey,
                                                      String query, int maxResults, int timeoutMs) {
        try {
            return executeOpenAiSearchStrict(engine, apiKey, query, maxResults, timeoutMs);
        } catch (Exception ex) {
            log.warn("自定义 OpenAI 联网检索异常 name={}: {}", engine.getName(), ex.getMessage());
            return List.of();
        }
    }

    private List<WebSearchResult> executeOpenAiSearchStrict(CustomWebSearchEngineConfig engine, String apiKey,
                                                            String query, int maxResults, int timeoutMs) throws Exception {
        String targetUrl = normalizeOpenAiUrl(engine.getEndpointUrl());
        URI uri = validatePublicEndpoint(targetUrl);

        JSONObject body = new JSONObject();
        body.set("model", StrUtil.blankToDefault(StrUtil.trim(engine.getModelName()), "deepseek-chat"));
        body.set("stream", false);
        body.set("temperature", 0.2);

        if (StrUtil.isNotBlank(engine.getExtraBodyJson())) {
            JSONObject extra;
            try {
                String trimmed = engine.getExtraBodyJson().trim();
                if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                    throw new BusinessException(400, "附加参数 JSON 必须是对象格式");
                }
                extra = JSONUtil.parseObj(trimmed);
            } catch (BusinessException be) {
                throw be;
            } catch (Exception ex) {
                throw new BusinessException(400, "附加参数 JSON 格式非法");
            }
            for (String key : extra.keySet()) {
                body.set(key, extra.get(key));
            }
        }

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content",
                "你是实时联网检索器。请针对用户问题检索最新互联网公开信息，输出精炼客观的事实摘要，"
                        + "并在末尾用 Markdown 列表附上真实可访问的参考链接（格式：- [网页标题](https://...)）。"
                        + "严禁编造未检索到的链接。"));
        messages.add(new JSONObject().set("role", "user").set("content", query));
        body.set("messages", messages);

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("User-Agent", "wu-admin-websearch/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        applyAuthHeader(req, engine, apiKey);

        HttpResponse<String> response = HTTP_CLIENT.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(502, "HTTP " + response.statusCode() + "：" + StrUtil.brief(response.body(), 180));
        }

        return parseOpenAiSearchResponse(response.body(), StrUtil.blankToDefault(engine.getName(), "DeepSeek 联网"), maxResults);
    }

    private List<WebSearchResult> parseOpenAiSearchResponse(String bodyStr, String engineName, int maxResults) {
        if (StrUtil.isBlank(bodyStr) || !JSONUtil.isTypeJSONObject(bodyStr)) {
            return List.of();
        }
        JSONObject root = JSONUtil.parseObj(bodyStr);
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return List.of();
        }
        JSONObject first = choices.getJSONObject(0);
        JSONObject msg = first != null ? first.getJSONObject("message") : null;
        String content = msg != null ? StrUtil.trimToEmpty(msg.getStr("content")) : "";
        if (StrUtil.isBlank(content)) {
            return List.of();
        }

        List<WebSearchResult> results = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        // 1. 优先提取返回正文中的 Markdown 链接与模型结构化 citations 字段
        JSONArray citations = root.getJSONArray("citations");
        if (citations != null) {
            for (int i = 0; i < citations.size() && results.size() < maxResults; i++) {
                Object item = citations.get(i);
                if (item instanceof String link && isHttpUrl(link) && seenUrls.add(link)) {
                    results.add(WebSearchResult.builder()
                            .title(engineName + " · 参考资料 " + (results.size() + 1))
                            .snippet(StrUtil.brief(content, 260))
                            .url(link)
                            .source(engineName)
                            .build());
                } else if (item instanceof JSONObject obj) {
                    String link = StrUtil.trimToEmpty(obj.getStr("url"));
                    if (isHttpUrl(link) && seenUrls.add(link)) {
                        results.add(WebSearchResult.builder()
                                .title(StrUtil.blankToDefault(obj.getStr("title"), engineName + " · 参考资料"))
                                .snippet(StrUtil.blankToDefault(obj.getStr("snippet"), StrUtil.brief(content, 260)))
                                .url(link)
                                .source(engineName)
                                .build());
                    }
                }
            }
        }

        Matcher mdMatcher = MD_LINK_PATTERN.matcher(content);
        while (mdMatcher.find() && results.size() < maxResults) {
            String title = mdMatcher.group(1).trim();
            String url = mdMatcher.group(2).trim();
            if (seenUrls.add(url)) {
                results.add(WebSearchResult.builder()
                        .title(title)
                        .snippet(StrUtil.brief(content, 280))
                        .url(url)
                        .source(engineName)
                        .build());
            }
        }

        if (results.isEmpty()) {
            Matcher urlMatcher = URL_PATTERN.matcher(content);
            while (urlMatcher.find() && results.size() < maxResults) {
                String url = urlMatcher.group().replaceAll("[),.;]+$", "");
                if (seenUrls.add(url)) {
                    results.add(WebSearchResult.builder()
                            .title(engineName + " · 检索来源 " + (results.size() + 1))
                            .snippet(StrUtil.brief(content, 280))
                            .url(url)
                            .source(engineName)
                            .build());
                }
            }
        }

        // 若接口返回了完整事实综述但未带外链 URL，仍保留为无外链参考摘要（供提示词增强，不伪造链接卡片）
        if (results.isEmpty()) {
            results.add(WebSearchResult.builder()
                    .title(engineName + " · 实时联网综述")
                    .snippet(StrUtil.brief(content, 900))
                    .url("")
                    .source(engineName)
                    .build());
        }
        return results;
    }

    private List<WebSearchResult> executeHttpJsonSearch(CustomWebSearchEngineConfig engine, String apiKey,
                                                        String query, int maxResults, int timeoutMs) {
        try {
            return executeHttpJsonSearchStrict(engine, apiKey, query, maxResults, timeoutMs);
        } catch (Exception ex) {
            log.warn("自定义 HTTP 联网检索异常 name={}: {}", engine.getName(), ex.getMessage());
            return List.of();
        }
    }

    private List<WebSearchResult> executeHttpJsonSearchStrict(CustomWebSearchEngineConfig engine, String apiKey,
                                                              String query, int maxResults, int timeoutMs) throws Exception {
        String method = StrUtil.blankToDefault(engine.getHttpMethod(), "POST").trim().toUpperCase();
        String endpoint = engine.getEndpointUrl().trim();
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String requestUrl = endpoint
                .replace("{{query}}", encodedQuery)
                .replace("{{limit}}", String.valueOf(maxResults));
        URI uri = validatePublicEndpoint(requestUrl);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Accept", "application/json")
                .header("User-Agent", "wu-admin-websearch/1.0");
        applyAuthHeader(reqBuilder, engine, apiKey);

        if ("GET".equals(method)) {
            if (!endpoint.contains("{{query}}")) {
                String template = StrUtil.trim(engine.getRequestTemplate());
                String querySuffix;
                if (StrUtil.isNotBlank(template)) {
                    querySuffix = template
                            .replace("{{query}}", encodedQuery)
                            .replace("{{limit}}", String.valueOf(maxResults));
                } else {
                    querySuffix = "q=" + encodedQuery;
                }
                String sep = requestUrl.contains("?") ? "&" : "?";
                uri = validatePublicEndpoint(requestUrl + sep + querySuffix);
                reqBuilder.uri(uri);
            }
            reqBuilder.GET();
        } else {
            String rawTpl = StrUtil.trim(engine.getRequestTemplate());
            String bodyPayload;
            if (StrUtil.isBlank(rawTpl)) {
                JSONObject fallback = new JSONObject();
                fallback.set("query", query);
                fallback.set("count", maxResults);
                bodyPayload = fallback.toString();
            } else {
                String escapedQuery = escapeJsonString(query);
                bodyPayload = rawTpl
                        .replace("{{query}}", escapedQuery)
                        .replace("{{limit}}", String.valueOf(maxResults));
                if (!JSONUtil.isTypeJSON(bodyPayload)) {
                    throw new BusinessException(400, "POST 请求体模板解析后不是合法 JSON");
                }
            }
            reqBuilder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyPayload, StandardCharsets.UTF_8));
        }

        HttpResponse<String> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(502, "HTTP " + response.statusCode() + "：" + StrUtil.brief(response.body(), 180));
        }
        return parseHttpJsonResponse(response.body(), engine, maxResults);
    }

    private List<WebSearchResult> parseHttpJsonResponse(String bodyStr, CustomWebSearchEngineConfig engine, int maxResults) {
        if (StrUtil.isBlank(bodyStr) || !JSONUtil.isTypeJSONObject(bodyStr)) {
            return List.of();
        }
        JSONObject root = JSONUtil.parseObj(bodyStr);
        String itemsPath = StrUtil.blankToDefault(StrUtil.trim(engine.getItemsPath()), "results");
        Object arrObj = readPath(root, itemsPath);
        if (!(arrObj instanceof JSONArray items) || items.isEmpty()) {
            return List.of();
        }

        String titleField = StrUtil.blankToDefault(StrUtil.trim(engine.getTitleField()), "title");
        String urlField = StrUtil.blankToDefault(StrUtil.trim(engine.getUrlField()), "url");
        String snippetField = StrUtil.blankToDefault(StrUtil.trim(engine.getSnippetField()), "content");
        String sourceName = StrUtil.blankToDefault(StrUtil.trim(engine.getName()), "自定义联网检索");

        List<WebSearchResult> results = new ArrayList<>();
        for (int i = 0; i < items.size() && results.size() < maxResults; i++) {
            JSONObject item = items.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String title = readStringPath(item, titleField);
            String url = readStringPath(item, urlField);
            String snippet = readStringPath(item, snippetField);
            if (StrUtil.isBlank(snippet)) {
                snippet = readStringPath(item, "snippet");
            }
            if (StrUtil.isBlank(snippet)) {
                snippet = readStringPath(item, "summary");
            }
            if (StrUtil.isBlank(title) || StrUtil.isBlank(snippet) || !isHttpUrl(url)) {
                continue;
            }
            results.add(WebSearchResult.builder()
                    .title(title.replaceAll("<[^>]*>", "").trim())
                    .snippet(snippet.trim())
                    .url(url.trim())
                    .source(sourceName)
                    .build());
        }
        return results;
    }

    private Object readPath(JSONObject root, String path) {
        if (root == null || StrUtil.isBlank(path)) {
            return null;
        }
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof JSONObject obj)) {
                return null;
            }
            current = obj.get(part);
        }
        return current;
    }

    private String readStringPath(JSONObject root, String path) {
        Object val = readPath(root, path);
        return val == null ? "" : String.valueOf(val);
    }

    private void applyAuthHeader(HttpRequest.Builder reqBuilder, CustomWebSearchEngineConfig engine, String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return;
        }
        String headerName = StrUtil.blankToDefault(StrUtil.trim(engine.getAuthHeader()), "Authorization");
        String prefix = engine.getAuthPrefix() != null ? engine.getAuthPrefix() : "Bearer ";
        reqBuilder.header(headerName, prefix + apiKey);
    }

    /** 按 id 从系统配置中心回读已保存的明文 API Key（防止前端提交脱敏占位符或留空后丢失） */
    private String resolveApiKey(CustomWebSearchEngineConfig engine) {
        String candidate = engine.getApiKey();
        if (StrUtil.isNotBlank(candidate) && !candidate.contains("•") && !candidate.trim().matches("^\\*+$")) {
            return candidate.trim();
        }
        if (StrUtil.isBlank(engine.getId())) {
            return "";
        }
        JSONObject thirdParty = systemConfigHelper.getGroupJson(SystemConfigHelper.GROUP_THIRD_PARTY);
        JSONObject webSearch = thirdParty != null ? thirdParty.getJSONObject("webSearch") : null;
        JSONArray providers = webSearch != null ? webSearch.getJSONArray("customProviders") : null;
        if (providers == null) {
            return "";
        }
        for (int i = 0; i < providers.size(); i++) {
            JSONObject item = providers.getJSONObject(i);
            if (item != null && engine.getId().equals(item.getStr("id"))) {
                return StrUtil.trimToEmpty(item.getStr("apiKey"));
            }
        }
        return "";
    }

    private CustomWebSearchEngineConfig copyWithApiKey(CustomWebSearchEngineConfig src, String apiKey) {
        CustomWebSearchEngineConfig copy = new CustomWebSearchEngineConfig();
        copy.setId(src.getId());
        copy.setName(src.getName());
        copy.setPreset(src.getPreset());
        copy.setProtocol(src.getProtocol());
        copy.setEnabled(src.getEnabled());
        copy.setPriority(src.getPriority());
        copy.setEndpointUrl(src.getEndpointUrl());
        copy.setHttpMethod(src.getHttpMethod());
        copy.setApiKey(apiKey);
        copy.setAuthHeader(src.getAuthHeader());
        copy.setAuthPrefix(src.getAuthPrefix());
        copy.setModelName(src.getModelName());
        copy.setExtraBodyJson(src.getExtraBodyJson());
        copy.setRequestTemplate(src.getRequestTemplate());
        copy.setItemsPath(src.getItemsPath());
        copy.setTitleField(src.getTitleField());
        copy.setUrlField(src.getUrlField());
        copy.setSnippetField(src.getSnippetField());
        copy.setTimeoutMs(src.getTimeoutMs());
        copy.setRemark(src.getRemark());
        return copy;
    }

    /**
     * 校验公网合法地址（复用 WebhookUrlValidator 防 SSRF）。
     * <p>联网搜索引擎由管理员手动配置，域名不做本机 DNS 解析：
     * 本机代理（TUN fake-ip）或 DNS 污染会把公网域名解析到 198.18.0.0/15 保留段，
     * 按解析结果判断会误杀 api.deepseek.com、api.tavily.com 等合法地址。</p>
     */
    public static URI validatePublicEndpoint(String url) {
        try {
            return WebhookUrlValidator.validateAllowingProxyDns(url);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage().replace("告警 webhook", "联网搜索接口地址") : "联网搜索接口地址非法";
            throw new BusinessException(400, msg);
        }
    }

    private String normalizeOpenAiUrl(String rawUrl) {
        String url = rawUrl.trim();
        if (url.endsWith("/chat/completions") || url.endsWith("/responses")) {
            return url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/v1")) {
            return url + "/chat/completions";
        }
        return url + "/v1/chat/completions";
    }

    private static int clampTimeout(Integer timeoutMs) {
        int val = timeoutMs != null ? timeoutMs : 10000;
        return Math.max(2000, Math.min(val, 30000));
    }

    private static String escapeJsonString(String input) {
        String quoted = JSONUtil.quote(input);
        if (quoted.length() >= 2 && quoted.startsWith("\"") && quoted.endsWith("\"")) {
            return quoted.substring(1, quoted.length() - 1);
        }
        return input;
    }

    private static boolean isHttpUrl(String url) {
        return StrUtil.isNotBlank(url) && (url.startsWith("http://") || url.startsWith("https://"));
    }
}
