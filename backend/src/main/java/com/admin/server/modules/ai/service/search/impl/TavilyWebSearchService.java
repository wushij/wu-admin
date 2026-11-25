package com.admin.server.modules.ai.service.search.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.ai.service.search.vo.TavilyUsageVO;
import com.admin.server.modules.ai.service.search.vo.WebSearchResult;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Tavily 大模型联网检索与官方额度查询服务（对标 EduMind 实现并集成至系统配置中心）
 */
@Service
public class TavilyWebSearchService {

    private static final Logger log = LoggerFactory.getLogger(TavilyWebSearchService.class);
    private static final String DEFAULT_TAVILY_BASE_URL = "https://api.tavily.com";
    private static final long USAGE_CACHE_TTL_MS = 5_000L;
    private static final int MAX_USAGE_CACHE_SIZE = 64;
    /** 连接超时：跨境链路首次 TLS 握手实测可达 3~4 秒，5 秒过于激进 */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** 检索请求超时：检索处于对话同步链路，不宜过长，失败后由聚合层走自定义引擎/DDG 兜底 */
    private static final Duration SEARCH_TIMEOUT = Duration.ofSeconds(15);
    /** 额度查询请求超时 */
    private static final Duration USAGE_TIMEOUT = Duration.ofSeconds(20);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static final List<String> DEFAULT_EXCLUDED_DOMAINS = List.of(
            "voachinese.com", "voanews.com", "rfa.org", "epochtimes.com",
            "ntdtv.com", "boxun.com", "secretchina.com", "falundafa.org", "minghui.org", "dajiyuan.com"
    );

    private static final Set<String> BLOCKED_DOMAIN_KEYWORDS = Set.of(
            "voachinese", "voanews", "rfa.org", "epochtimes", "ntdtv", "boxun", "secretchina", "dajiyuan"
    );

    private static final ConcurrentHashMap<String, CachedUsage> USAGE_CACHE = new ConcurrentHashMap<>();

    private record CachedUsage(TavilyUsageVO vo, long expireAt) {
    }

    @Resource
    private SystemConfigHelper systemConfigHelper;

    /** 获取当前生效的 Tavily 配置 */
    public RuntimeTavilyConfig getEffectiveConfig() {
        JSONObject thirdParty = systemConfigHelper.getGroupJson(SystemConfigHelper.GROUP_THIRD_PARTY);
        JSONObject tavily = thirdParty != null ? thirdParty.getJSONObject("tavily") : null;

        boolean enabled = tavily != null && Boolean.TRUE.equals(tavily.getBool("enabled", false));
        String apiKey = tavily != null ? StrUtil.trimToEmpty(tavily.getStr("apiKey")) : "";
        if (StrUtil.isBlank(apiKey)) {
            String envKey = System.getenv("TAVILY_API_KEY");
            if (StrUtil.isNotBlank(envKey)) {
                apiKey = envKey.trim();
            }
        }
        String searchDepth = tavily != null ? StrUtil.blankToDefault(StrUtil.trim(tavily.getStr("searchDepth")), "basic") : "basic";
        if (!"advanced".equalsIgnoreCase(searchDepth)) {
            searchDepth = "basic";
        } else {
            searchDepth = "advanced";
        }
        String topic = tavily != null ? StrUtil.blankToDefault(StrUtil.trim(tavily.getStr("topic")), "general") : "general";
        if (!"news".equalsIgnoreCase(topic)) {
            topic = "general";
        } else {
            topic = "news";
        }
        boolean includeAnswer = tavily == null || Boolean.TRUE.equals(tavily.getBool("includeAnswer", true));
        int maxResults = tavily != null ? tavily.getInt("maxResults", 5) : 5;
        maxResults = Math.max(1, Math.min(maxResults, 10));

        // 仅允许通过环境变量覆盖官方受信任域名，防止数据库改写导致 SSRF 与密钥泄露
        String baseUrl = resolveSafeBaseUrl(System.getenv("TAVILY_BASE_URL"));
        return new RuntimeTavilyConfig(enabled, apiKey, searchDepth, topic, includeAnswer, baseUrl, maxResults);
    }

    /** 执行 Tavily 联网检索 */
    public List<WebSearchResult> search(String query, int limit) {
        return search(query, limit, null);
    }

    /** 执行 Tavily 联网检索（支持指定时效范围，空命中时自动退避取消 time_range 重试一次） */
    public List<WebSearchResult> search(String query, int limit, String preferredTimeRange) {
        if (StrUtil.isBlank(query)) {
            return List.of();
        }
        RuntimeTavilyConfig config = getEffectiveConfig();
        if (!config.enabled()) {
            return List.of();
        }
        int maxResults = limit > 0 ? Math.min(limit, 10) : config.maxResults();
        String normalizedQuery = normalizeSearchQuery(query);
        String timeRange = resolveEffectiveTimeRange(normalizedQuery, preferredTimeRange);

        List<WebSearchResult> results = executeSearchRequest(config, normalizedQuery, maxResults, timeRange);
        if (results.isEmpty() && StrUtil.isNotBlank(timeRange)) {
            // 指定时间窗口未命中或均被反爬页过滤时，取消时间窗口限制再检索一次提升召回率
            results = executeSearchRequest(config, normalizedQuery, maxResults, null);
        }
        return results;
    }

    private List<WebSearchResult> executeSearchRequest(RuntimeTavilyConfig config, String searchQuery,
                                                     int maxResults, String timeRange) {
        List<WebSearchResult> list = new ArrayList<>();
        try {
            JSONObject body = new JSONObject();
            boolean hasKey = StrUtil.isNotBlank(config.apiKey());
            if (hasKey) {
                body.set("api_key", config.apiKey());
            }
            body.set("query", searchQuery);
            body.set("search_depth", config.searchDepth());
            body.set("topic", config.topic());
            if (StrUtil.isNotBlank(timeRange)) {
                body.set("time_range", timeRange);
            }
            body.set("include_answer", config.includeAnswer());
            body.set("include_images", false);
            body.set("include_raw_content", false);
            body.set("max_results", maxResults);
            body.set("exclude_domains", DEFAULT_EXCLUDED_DOMAINS);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/search"))
                    .timeout(SEARCH_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "wu-admin-websearch/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
            if (!hasKey) {
                reqBuilder.header("X-Tavily-Access-Mode", "keyless");
            }

            HttpResponse<String> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300 && StrUtil.isNotBlank(response.body())) {
                JSONObject respJson = JSONUtil.parseObj(response.body());
                JSONArray results = respJson.getJSONArray("results");
                if (results != null) {
                    for (int i = 0; i < results.size(); i++) {
                        JSONObject item = results.getJSONObject(i);
                        if (item == null) {
                            continue;
                        }
                        String title = cleanTitle(item.getStr("title"));
                        String content = StrUtil.trimToEmpty(item.getStr("content"));
                        String url = StrUtil.trimToEmpty(item.getStr("url"));
                        Double score = item.getDouble("score");
                        if (StrUtil.isBlank(title) || StrUtil.isBlank(content) || !isHttpUrl(url)) {
                            continue;
                        }
                        if (!isSafeWebResult(url, title, content)) {
                            continue;
                        }
                        list.add(WebSearchResult.builder()
                                .title(title)
                                .snippet(content)
                                .url(url)
                                .source("Tavily 检索")
                                .score(score)
                                .build());
                    }
                }
            } else {
                log.warn("Tavily 联网检索返回非成功状态: status={}", response.statusCode());
            }
        } catch (Exception ex) {
            log.warn("Tavily 联网检索调用异常: query={}, error={}", searchQuery, ex.getMessage());
        }
        return list;
    }

    /** 查询 Tavily 官方账户额度与连通状态 */
    public TavilyUsageVO queryUsage(String candidateKey) {
        String resolvedKey = resolveCandidateOrSavedKey(candidateKey);
        if (StrUtil.isBlank(resolvedKey)) {
            return TavilyUsageVO.builder()
                    .valid(false)
                    .message("未配置有效 API Key，请填入或保存密钥后查询")
                    .usage(0)
                    .limit(0)
                    .remaining(0)
                    .percent(0.0)
                    .build();
        }

        String usageUrl = resolveSafeBaseUrl(System.getenv("TAVILY_BASE_URL")) + "/usage";
        long now = System.currentTimeMillis();
        USAGE_CACHE.entrySet().removeIf(entry -> now >= entry.getValue().expireAt());
        String cacheKey = sha256CacheKey(resolvedKey, usageUrl);
        CachedUsage cached = USAGE_CACHE.get(cacheKey);
        if (cached != null && now < cached.expireAt()) {
            return cached.vo();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(usageUrl))
                .timeout(USAGE_TIMEOUT)
                .header("Authorization", "Bearer " + resolvedKey)
                .header("User-Agent", "wu-admin-config/1.0")
                .GET()
                .build();

        HttpResponse<String> resp = null;
        Exception lastError = null;
        // 该接口是幂等 GET，跨境链路偶发 TLS 握手超时，重试一次即可显著提升成功率
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                break;
            } catch (Exception ex) {
                lastError = ex;
                log.warn("查询 Tavily 额度失败（第 {} 次尝试）: {}", attempt, ex.toString());
                if (attempt < 2) {
                    sleepQuietly(500L);
                }
            }
        }

        if (resp == null) {
            String reason = (lastError instanceof HttpConnectTimeoutException || lastError instanceof HttpTimeoutException)
                    ? "连接 Tavily 官方接口超时，请检查本机或服务器的网络与代理设置"
                    : "连接 Tavily 官方接口失败：" + (lastError == null ? "未知错误" : lastError.getMessage());
            return buildUnavailable(reason);
        }
        if (resp.statusCode() >= 200 && resp.statusCode() < 300 && StrUtil.isNotBlank(resp.body())) {
            TavilyUsageVO vo = parseUsageBody(resp.body());
            if (USAGE_CACHE.size() >= MAX_USAGE_CACHE_SIZE) {
                USAGE_CACHE.clear();
            }
            USAGE_CACHE.put(cacheKey, new CachedUsage(vo, now + USAGE_CACHE_TTL_MS));
            return vo;
        }
        log.warn("Tavily 额度接口返回非成功状态: status={}", resp.statusCode());
        return buildUnavailable(describeHttpFailure(resp.statusCode()));
    }

    /** 统一构造不可用结果，避免各失败分支重复拼装 VO */
    private static TavilyUsageVO buildUnavailable(String message) {
        return TavilyUsageVO.builder()
                .valid(false)
                .message(message)
                .usage(0)
                .limit(0)
                .remaining(0)
                .percent(0.0)
                .build();
    }

    /** 将 HTTP 状态码翻译为可操作的提示文案 */
    private static String describeHttpFailure(int status) {
        if (status == 401 || status == 403) {
            return "Tavily 鉴权失败 (HTTP " + status + ")，请确认 API Key 是否有效";
        }
        if (status == 429) {
            return "Tavily 请求频率超限或额度已耗尽 (HTTP " + status + ")";
        }
        return "Tavily 额度接口不可用 (HTTP " + status + ")";
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private TavilyUsageVO parseUsageBody(String bodyStr) {
        JSONObject body = JSONUtil.parseObj(bodyStr);
        JSONObject keyObj = body.getJSONObject("key");
        JSONObject accountObj = body.getJSONObject("account");

        // Tavily 同时返回 key 维度（单密钥）与 account 维度（账户汇总），account 更完整，优先取账户维度
        Integer accountUsage = readInt(accountObj, "plan_usage");
        Integer keyUsage = readInt(keyObj, "usage");
        int planUsage = intOrDefault(accountUsage, intOrDefault(keyUsage, 0));

        Integer accountLimit = readInt(accountObj, "plan_limit");
        Integer keyLimit = readInt(keyObj, "limit");
        Integer limit = resolvePositive(accountLimit, keyLimit);

        String currentPlan = accountObj != null ? StrUtil.nullToEmpty(accountObj.getStr("current_plan")) : "";
        int searchUsage = intOrDefault(readInt(keyObj, "search_usage"), planUsage);
        int crawlUsage = intOrDefault(readInt(keyObj, "crawl_usage"), 0);
        int extractUsage = intOrDefault(readInt(keyObj, "extract_usage"), 0);
        int mapUsage = intOrDefault(readInt(keyObj, "map_usage"), 0);
        int researchUsage = intOrDefault(readInt(keyObj, "research_usage"), 0);
        int paygoUsage = intOrDefault(readInt(accountObj, "paygo_usage"), 0);
        Integer paygoLimit = resolvePositive(readInt(accountObj, "paygo_limit"), null);

        Integer remaining = limit != null ? Math.max(0, limit - planUsage) : null;
        double percent = (limit != null && limit > 0)
                ? Math.min(100.0, Math.round(planUsage * 1000.0 / limit) / 10.0)
                : 0.0;

        return TavilyUsageVO.builder()
                .valid(true)
                .message("连通成功，已同步 Tavily 官方账户额度")
                .usage(planUsage)
                .limit(limit)
                .remaining(remaining)
                .percent(percent)
                .currentPlan(currentPlan)
                .searchUsage(searchUsage)
                .crawlUsage(crawlUsage)
                .extractUsage(extractUsage)
                .mapUsage(mapUsage)
                .researchUsage(researchUsage)
                .paygoUsage(paygoUsage)
                .paygoLimit(paygoLimit)
                .build();
    }

    /** 安全读取整型字段：字段缺失或值为 JSON null 时返回 null，避免解析异常导致整体失败 */
    private static Integer readInt(JSONObject obj, String key) {
        if (obj == null || !obj.containsKey(key)) {
            return null;
        }
        Object raw = obj.get(key);
        if (raw == null || raw instanceof JSONNull) {
            return null;
        }
        try {
            return obj.getInt(key);
        } catch (Exception ex) {
            return null;
        }
    }

    private static int intOrDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    /** 在候选配额中取第一个大于 0 的值，全部无效时返回 null（表示官方未返回明确上限） */
    private static Integer resolvePositive(Integer first, Integer second) {
        if (first != null && first > 0) {
            return first;
        }
        if (second != null && second > 0) {
            return second;
        }
        return null;
    }

    private String resolveCandidateOrSavedKey(String candidateKey) {
        if (StrUtil.isNotBlank(candidateKey) && !candidateKey.contains("•") && !candidateKey.trim().matches("^\\*+$")) {
            return candidateKey.trim();
        }
        RuntimeTavilyConfig cfg = getEffectiveConfig();
        return cfg.apiKey();
    }

    public static String resolveSafeBaseUrl(String envBaseUrl) {
        if (StrUtil.isNotBlank(envBaseUrl)) {
            try {
                URI uri = URI.create(envBaseUrl.trim());
                String host = uri.getHost();
                if ("https".equalsIgnoreCase(uri.getScheme()) && host != null) {
                    String lower = host.toLowerCase(Locale.ROOT);
                    if ("api.tavily.com".equals(lower) || lower.endsWith(".tavily.com")) {
                        String trimmed = envBaseUrl.trim();
                        while (trimmed.endsWith("/")) {
                            trimmed = trimmed.substring(0, trimmed.length() - 1);
                        }
                        return trimmed;
                    }
                }
            } catch (Exception ignored) {
                // 非法域名忽略并回退默认官方地址
            }
        }
        return DEFAULT_TAVILY_BASE_URL;
    }

    private static String sha256CacheKey(String rawKey, String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((rawKey + "@" + url).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return Integer.toHexString(Objects.hash(rawKey, url));
        }
    }

    private static boolean isHttpUrl(String url) {
        return StrUtil.isNotBlank(url) && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private static String cleanTitle(String rawTitle) {
        if (StrUtil.isBlank(rawTitle)) {
            return "网络检索资讯";
        }
        return rawTitle.replaceAll("<[^>]*>", "").trim();
    }

    private static final List<String> ANTI_BOT_TITLES_OR_SNIPPETS = List.of(
            "安全验证中", "正在验证您的浏览器", "browser verification", "checking your browser",
            "just a moment", "verify you are human", "access denied", "403 forbidden",
            "enable javascript and cookies", "captcha"
    );

    public static boolean isSafeWebResult(String url, String title, String content) {
        String safeUrl = StrUtil.nullToEmpty(url);
        String lowerUrl = safeUrl.toLowerCase(Locale.ROOT);
        for (String kw : BLOCKED_DOMAIN_KEYWORDS) {
            if (lowerUrl.contains(kw)) {
                return false;
            }
        }
        String safeTitle = StrUtil.nullToEmpty(title);
        if (safeTitle.contains("美国之音") || safeTitle.contains("自由亚洲")
                || safeTitle.contains("大纪元") || safeTitle.contains("新唐人")) {
            return false;
        }
        String combinedLower = (safeTitle + " " + StrUtil.nullToEmpty(content)).toLowerCase(Locale.ROOT);
        for (String noise : ANTI_BOT_TITLES_OR_SNIPPETS) {
            if (combinedLower.contains(noise)) {
                return false;
            }
        }
        return true;
    }

    private static final Pattern CONVERSATIONAL_PREFIX = Pattern.compile("^(请问|帮我查一下|帮我搜一下|查一下|搜索一下|想知道)+[，,：:\\s]*");
    private static final Pattern CONVERSATIONAL_SUFFIX = Pattern.compile("[，,。.!！？?\\s]*(怎么样|如何|好不好|吗|呢|呀|吧)+[？?！!。]*$");

    /** 清洗口语前后缀，保留干净的检索关键词（不再硬拼 ISO 日期串以免误召回日历/车票列表页） */
    public static String normalizeSearchQuery(String query) {
        if (StrUtil.isBlank(query)) {
            return "";
        }
        String trimmed = query.trim();
        String cleaned = CONVERSATIONAL_PREFIX.matcher(trimmed).replaceFirst("");
        cleaned = CONVERSATIONAL_SUFFIX.matcher(cleaned).replaceFirst("").trim();
        return StrUtil.isNotBlank(cleaned) ? cleaned : trimmed;
    }

    private static String resolveEffectiveTimeRange(String query, String preferredTimeRange) {
        if (StrUtil.isNotBlank(preferredTimeRange)) {
            String normalized = preferredTimeRange.trim().toLowerCase(Locale.ROOT);
            if ("none".equals(normalized)) {
                return null;
            }
            if ("day".equals(normalized) || "week".equals(normalized) || "month".equals(normalized)) {
                return normalized;
            }
        }
        return resolveTimeRange(query);
    }

    /**
     * 按提问时效词推导默认 time_range：
     * 严格突发新闻词使用 day；日常今天/天气/近期词使用 week，避免 24h 窗口过窄漏掉气象与生活服务页面。
     */
    public static String resolveTimeRange(String query) {
        if (StrUtil.isBlank(query)) {
            return null;
        }
        if (query.contains("刚刚") || query.contains("突发") || query.contains("今日新闻") || query.contains("最新消息")) {
            return "day";
        }
        if (query.contains("今天") || query.contains("今日") || query.contains("本周")
                || query.contains("这周") || query.contains("近期") || query.contains("最近")
                || query.contains("最新") || query.contains("天气")) {
            return "week";
        }
        return null;
    }

    public record RuntimeTavilyConfig(
            boolean enabled,
            String apiKey,
            String searchDepth,
            String topic,
            boolean includeAnswer,
            String baseUrl,
            int maxResults
    ) {
    }
}
