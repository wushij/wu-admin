package com.admin.server.modules.ai.providers;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 大模型协议实现共用的 HTTP 工具。
 * <p>负责路径模板解析、额外请求头注入、错误体读取与统一的错误消息格式。</p>
 */
final class ProviderHttpUtils {

    private static final Logger log = LoggerFactory.getLogger(ProviderHttpUtils.class);

    /** Anthropic 要求的协议版本头，2023-06-01 为长期稳定版本 */
    static final String ANTHROPIC_VERSION = "2023-06-01";

    /** 模型列表属于轻量元数据查询，单独使用短超时的连接池 */
    private static final HttpClient MODELS_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private ProviderHttpUtils() {
    }

    /**
     * 解析请求地址。
     *
     * @param baseUrl       API 基础地址
     * @param pathTemplate  自定义路径模板，支持 {model} 占位；以 http(s) 开头时视为完整地址
     * @param modelName     模型名，用于替换 {model}
     * @param defaultSuffix 协议内置后缀，如 /chat/completions、/v1/messages
     */
    static String resolveUrl(String baseUrl, String pathTemplate, String modelName, String defaultSuffix) {
        String base = StrUtil.removeSuffix(StrUtil.trim(baseUrl), "/");
        String template = StrUtil.trim(pathTemplate);
        if (StrUtil.isNotBlank(template)) {
            String path = template.replace("{model}", StrUtil.nullToEmpty(modelName));
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return path;
            }
            return joinUrl(base, path);
        }
        if (base.endsWith(defaultSuffix)) {
            return base;
        }
        // 用户填了 .../v1 时避免拼成 /v1/v1/messages
        if (defaultSuffix.startsWith("/v1/") && base.endsWith("/v1")) {
            return base + defaultSuffix.substring(3);
        }
        return base + defaultSuffix;
    }

    static String joinUrl(String base, String path) {
        return StrUtil.removeSuffix(StrUtil.trim(base), "/") + "/" + StrUtil.removePrefix(StrUtil.trim(path), "/");
    }

    /**
     * 解析「获取模型列表」的候选地址。
     * <p>OpenAI 兼容厂商的标准端点多为 {@code {baseUrl}/models}，但 baseUrl 常被填成
     * {@code .../v1}、甚至直接填对话端点，这里先剥掉对话路径，再按协议拼列表端点；
     * 同时给出 {@code .../v1/models} 作为兜底（如 OpenAI 官方裸域名场景）。</p>
     */
    static List<String> resolveModelsUrls(String baseUrl, String protocol) {
        String base = StrUtil.removeSuffix(StrUtil.trim(baseUrl), "/");
        for (String suffix : List.of("/chat/completions", "/completions", "/v1/messages", "/messages")) {
            if (base.endsWith(suffix)) {
                base = StrUtil.removeSuffix(base, suffix);
                break;
            }
        }
        List<String> urls = new ArrayList<>(2);
        urls.add(resolveUrl(base, null, null,
                "anthropic".equalsIgnoreCase(protocol) ? "/v1/models" : "/models"));
        // 裸域名下部分网关只暴露 /v1/models，作为第二候选；已带 /v1 时该地址与首选一致，去重
        String fallback = resolveUrl(base, null, null, "/v1/models");
        if (!urls.contains(fallback)) {
            urls.add(fallback);
        }
        return urls;
    }

    /**
     * 拉取供应商模型列表。
     * <p>依次尝试候选地址，HTTP 200 但内容为空时也会继续尝试下一个，
     * 全部失败后抛出最后一次的错误，便于用户定位是地址还是密钥问题。</p>
     */
    static List<String> fetchModelIds(String baseUrl, String protocol, String extraHeaders,
                                      String plainApiKey) throws Exception {
        BusinessException lastError = null;
        for (String url : resolveModelsUrls(baseUrl, protocol)) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET();
            if ("anthropic".equalsIgnoreCase(protocol)) {
                builder.header("anthropic-version", ANTHROPIC_VERSION);
                if (StrUtil.isNotBlank(plainApiKey)) {
                    builder.header("x-api-key", plainApiKey);
                }
            } else {
                builder.header("Authorization", "Bearer " + StrUtil.nullToEmpty(plainApiKey));
            }
            applyExtraHeaders(builder, extraHeaders);

            HttpResponse<String> response = MODELS_CLIENT.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                List<String> models = parseModelIds(response.body());
                if (!models.isEmpty()) {
                    return models;
                }
                lastError = new BusinessException("接口未返回任何模型");
                continue;
            }
            lastError = new BusinessException("获取模型列表失败(HTTP " + response.statusCode() + "): "
                    + StrUtil.brief(response.body(), 300));
        }
        throw lastError != null ? lastError : new BusinessException("获取模型列表失败");
    }

    /**
     * 从 /models 响应里提取模型标识。
     * <p>兼容 OpenAI 的 {@code {"data":[{"id":...}]}}、Ollama 的
     * {@code {"models":[{"name":...}]}}、部分网关直接返回数组或纯字符串数组的写法。</p>
     */
    static List<String> parseModelIds(String body) {
        List<String> ids = new ArrayList<>();
        if (StrUtil.isBlank(body)) {
            return ids;
        }
        // Hutool 对 HTML 错误页等非 JSON 文本会宽容地解析成空数组，先按首字符快速甄别
        String trimmed = body.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            throw new BusinessException("模型列表响应不是合法 JSON: " + StrUtil.brief(body, 200));
        }
        JSONArray array = null;
        try {
            Object parsed = JSONUtil.parse(trimmed);
            if (parsed instanceof JSONArray) {
                array = (JSONArray) parsed;
            } else if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;
                for (String key : List.of("data", "models", "result", "items")) {
                    Object value = obj.get(key);
                    if (value instanceof JSONArray) {
                        array = (JSONArray) value;
                        break;
                    }
                }
                if (array == null) {
                    collectModelId(obj, ids);
                }
            } else {
                // 部分网关出错时会返回 HTML 错误页，Hutool 会解析成普通字符串而非抛异常
                throw new BusinessException("模型列表响应格式不受支持: " + StrUtil.brief(body, 200));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("模型列表响应不是合法 JSON: " + StrUtil.brief(body, 200));
        }
        if (array != null) {
            for (Object item : array) {
                if (item instanceof JSONObject) {
                    collectModelId((JSONObject) item, ids);
                } else if (item != null && StrUtil.isNotBlank(String.valueOf(item))) {
                    addUnique(ids, String.valueOf(item).trim());
                }
            }
        }
        return ids;
    }

    private static void collectModelId(JSONObject item, List<String> ids) {
        for (String key : List.of("id", "name", "model")) {
            String value = item.getStr(key);
            if (StrUtil.isNotBlank(value)) {
                addUnique(ids, value.trim());
                return;
            }
        }
    }

    private static void addUnique(List<String> ids, String id) {
        if (StrUtil.isNotBlank(id) && !ids.contains(id)) {
            ids.add(id);
        }
    }

    /**
     * 注入用户配置的额外请求头。
     * <p>鉴权头由协议实现负责，这里禁止覆盖，避免配置错误导致密钥丢失。</p>
     */
    static void applyExtraHeaders(HttpRequest.Builder builder, String extraHeadersJson) {
        if (StrUtil.isBlank(extraHeadersJson)) {
            return;
        }
        try {
            JSONObject headers = JSONUtil.parseObj(extraHeadersJson);
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                String name = entry.getKey();
                if (StrUtil.isBlank(name) || isReservedHeader(name)) {
                    continue;
                }
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                builder.header(name, value);
            }
        } catch (Exception e) {
            log.warn("AI 模型额外请求头 JSON 解析失败，已忽略: {}", e.getMessage());
        }
    }

    private static boolean isReservedHeader(String name) {
        String lower = name.toLowerCase();
        return "authorization".equals(lower) || "x-api-key".equals(lower)
                || "api-key".equals(lower) || "content-type".equals(lower);
    }

    static String readErrorBody(InputStream body) {
        try (InputStream is = body) {
            return StrUtil.brief(new String(is.readAllBytes(), StandardCharsets.UTF_8), 300);
        } catch (Exception e) {
            return "";
        }
    }

    static BusinessException httpError(int status, String body) {
        return new BusinessException("大模型接口调用失败(HTTP " + status + "): " + StrUtil.brief(body, 300));
    }

    /** 兼容 OpenAI 的 {"error":{"message":...}} 与 Anthropic 的 {"error":{"type":...,"message":...}} */
    static String extractErrorMessage(Object error) {
        if (error instanceof JSONObject) {
            JSONObject obj = (JSONObject) error;
            String message = obj.getStr("message");
            if (StrUtil.isBlank(message)) {
                message = obj.toString();
            }
            return StrUtil.brief(message, 300);
        }
        return StrUtil.brief(String.valueOf(error), 300);
    }
}
