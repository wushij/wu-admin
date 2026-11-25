package com.admin.server.modules.ai.service.search.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.ai.service.search.WebSearchService;
import com.admin.server.modules.ai.service.search.vo.CustomWebSearchEngineConfig;
import com.admin.server.modules.ai.service.search.vo.WebSearchResult;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 聚合联网搜索门面服务
 * <p>
 * 按系统配置中的联网总开关、检索策略（自定义引擎优先 / Tavily 优先）与优先级顺序调度：
 * <ol>
 *   <li>已启用的自定义联网引擎列表（支持 DeepSeek 联网、博查、Serper、SearXNG、自定义 HTTP 等，按 priority 升序）</li>
 *   <li>Tavily 大模型官方联网搜索引擎</li>
 *   <li>DuckDuckGo 免 Key 轻量网页兜底（可配置开关）</li>
 * </ol>
 * </p>
 */
@Primary
@Service
public class AggregatedWebSearchService implements WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(AggregatedWebSearchService.class);

    private static final HttpClient DDG_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Resource
    private TavilyWebSearchService tavilyWebSearchService;

    @Resource
    private CustomWebSearchExecutor customWebSearchExecutor;

    @Override
    public List<WebSearchResult> search(String query, int limit) {
        return search(query, limit, null);
    }

    @Override
    public List<WebSearchResult> search(String query, int limit, String preferredTimeRange) {
        if (StrUtil.isBlank(query)) {
            return List.of();
        }
        WebSearchSettings settings = loadSettings();
        if (!settings.enabled()) {
            log.debug("联网搜索全局总开关未开启，跳过检索 query={}", query);
            return List.of();
        }

        String cleanedQuery = TavilyWebSearchService.normalizeSearchQuery(query);
        int maxResults = limit > 0 ? Math.min(limit, 8) : settings.maxCitations();
        boolean tavilyFirst = "tavily_first".equalsIgnoreCase(settings.strategy());

        if (tavilyFirst) {
            List<WebSearchResult> tavilyResults = tavilyWebSearchService.search(cleanedQuery, maxResults, preferredTimeRange);
            if (!tavilyResults.isEmpty()) {
                return tavilyResults;
            }
            List<WebSearchResult> customResults = filterNoiseResults(tryCustomEngines(settings.customProviders(), cleanedQuery, maxResults));
            if (!customResults.isEmpty()) {
                return customResults;
            }
        } else {
            List<WebSearchResult> customResults = filterNoiseResults(tryCustomEngines(settings.customProviders(), cleanedQuery, maxResults));
            if (!customResults.isEmpty()) {
                return customResults;
            }
            List<WebSearchResult> tavilyResults = tavilyWebSearchService.search(cleanedQuery, maxResults, preferredTimeRange);
            if (!tavilyResults.isEmpty()) {
                return tavilyResults;
            }
        }

        if (settings.duckduckgoFallback()) {
            List<WebSearchResult> ddgResults = filterNoiseResults(searchDuckDuckGo(cleanedQuery, maxResults));
            if (!ddgResults.isEmpty()) {
                return ddgResults;
            }
        }
        return List.of();
    }

    private List<WebSearchResult> filterNoiseResults(List<WebSearchResult> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<WebSearchResult> filtered = new ArrayList<>(raw.size());
        for (WebSearchResult item : raw) {
            if (item == null || !TavilyWebSearchService.isSafeWebResult(item.getUrl(), item.getTitle(), item.getSnippet())) {
                continue;
            }
            filtered.add(item);
        }
        return filtered;
    }

    @Override
    public boolean isCapabilityEnabled() {
        WebSearchSettings settings = loadSettings();
        if (!settings.enabled()) {
            return false;
        }
        boolean hasCustom = !settings.customProviders().isEmpty();
        boolean hasTavily = tavilyWebSearchService.getEffectiveConfig().enabled();
        return hasCustom || hasTavily || settings.duckduckgoFallback();
    }

    @Override
    public String resolveActiveSummary() {
        WebSearchSettings settings = loadSettings();
        if (!settings.enabled()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        if ("tavily_first".equalsIgnoreCase(settings.strategy())) {
            if (tavilyWebSearchService.getEffectiveConfig().enabled()) {
                names.add("Tavily");
            }
            for (CustomWebSearchEngineConfig engine : settings.customProviders()) {
                if (StrUtil.isNotBlank(engine.getName())) {
                    names.add(engine.getName().trim());
                }
            }
        } else {
            for (CustomWebSearchEngineConfig engine : settings.customProviders()) {
                if (StrUtil.isNotBlank(engine.getName())) {
                    names.add(engine.getName().trim());
                }
            }
            if (tavilyWebSearchService.getEffectiveConfig().enabled()) {
                names.add("Tavily");
            }
        }
        if (names.isEmpty() && settings.duckduckgoFallback()) {
            names.add("DuckDuckGo 兜底");
        }
        return String.join(" · ", names);
    }

    @Override
    public String formatSearchResultsForPrompt(List<WebSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        String retrievedAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm (EEEE)", Locale.SIMPLIFIED_CHINESE));
        StringBuilder sb = new StringBuilder("\n<web_search_context>\n");
        sb.append("本次联网检索执行时间：").append(retrievedAt).append('\n');
        sb.append("以下是系统刚刚从互联网检索到的参考网页（网页快照可能存在缓存滞后，若页面中日期早于当前系统时间，请以 <current_time> 为准甄别时效性）：\n");
        for (int i = 0; i < results.size(); i++) {
            WebSearchResult r = results.get(i);
            String urlPart = StrUtil.isNotBlank(r.getUrl()) ? " (" + r.getUrl() + ")" : "";
            sb.append(String.format("[%d] 标题: %s\n    来源: %s%s\n    摘要: %s\n",
                    i + 1,
                    StrUtil.nullToEmpty(r.getTitle()),
                    StrUtil.blankToDefault(r.getSource(), "联网检索"),
                    urlPart,
                    StrUtil.nullToEmpty(r.getSnippet())));
        }
        sb.append("</web_search_context>\n");
        return sb.toString();
    }

    @Override
    public String buildFallbackPrompt(String query) {
        if (StrUtil.isBlank(query) || !isCapabilityEnabled()) {
            return "";
        }
        return "\n<web_search_fallback>\n"
                + "用户开启了联网搜索，但当前外部检索通道暂未命中可直接引用的网页切片。请结合已知事实客观作答，若涉及实时最新动态请如实说明联网检索暂未获取到明确来源，切勿编造网页链接。\n"
                + "</web_search_fallback>\n";
    }

    private List<WebSearchResult> tryCustomEngines(List<CustomWebSearchEngineConfig> providers, String query, int limit) {
        for (CustomWebSearchEngineConfig engine : providers) {
            List<WebSearchResult> res = customWebSearchExecutor.execute(engine, query, limit);
            if (!res.isEmpty()) {
                return res;
            }
        }
        return List.of();
    }

    private WebSearchSettings loadSettings() {
        JSONObject thirdParty = systemConfigHelper.getGroupJson(SystemConfigHelper.GROUP_THIRD_PARTY);
        JSONObject ws = thirdParty != null ? thirdParty.getJSONObject("webSearch") : null;
        JSONObject tavily = thirdParty != null ? thirdParty.getJSONObject("tavily") : null;

        boolean tavilyEnabled = tavily != null && Boolean.TRUE.equals(tavily.getBool("enabled", false));
        // 若管理员尚未配置过 webSearch.enabled，但已开启 Tavily，视为总开关就绪
        boolean enabled = ws != null
                ? Boolean.TRUE.equals(ws.getBool("enabled", tavilyEnabled))
                : tavilyEnabled;
        String strategy = ws != null ? StrUtil.blankToDefault(ws.getStr("strategy"), "custom_first") : "custom_first";
        boolean duckduckgoFallback = ws != null && Boolean.TRUE.equals(ws.getBool("duckduckgoFallback", false));
        int maxCitations = ws != null ? Math.max(1, Math.min(ws.getInt("maxCitations", 5), 10)) : 5;

        List<CustomWebSearchEngineConfig> enabledCustom = new ArrayList<>();
        JSONArray arr = ws != null ? ws.getJSONArray("customProviders") : null;
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                CustomWebSearchEngineConfig cfg = JSONUtil.toBean(item, CustomWebSearchEngineConfig.class);
                if (Boolean.TRUE.equals(cfg.getEnabled()) && StrUtil.isNotBlank(cfg.getEndpointUrl())) {
                    enabledCustom.add(cfg);
                }
            }
        }
        enabledCustom.sort(Comparator.comparingInt(c -> c.getPriority() != null ? c.getPriority() : 100));
        return new WebSearchSettings(enabled, strategy, duckduckgoFallback, maxCitations, enabledCustom);
    }

    private List<WebSearchResult> searchDuckDuckGo(String query, int maxResults) {
        List<WebSearchResult> list = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            URI targetUri = URI.create("https://html.duckduckgo.com/html/?q=" + encodedQuery);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .GET()
                    .build();
            HttpResponse<String> response = DDG_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300 && StrUtil.isNotBlank(response.body())) {
                list = parseDuckDuckGoHtml(response.body(), maxResults);
            }
        } catch (Exception ex) {
            log.warn("DuckDuckGo 备用检索失败: query={}, error={}", query, ex.getMessage());
        }
        return list;
    }

    private List<WebSearchResult> parseDuckDuckGoHtml(String html, int max) {
        List<WebSearchResult> results = new ArrayList<>();
        String[] blocks = html.split("class=\"result\\s+results_links");
        for (int i = 1; i < blocks.length && results.size() < max; i++) {
            String block = blocks[i];
            String title = cleanHtml(extractBetween(block, "class=\"result__title\">", "</a>"));
            String snippet = cleanHtml(extractBetween(block, "class=\"result__snippet\">", "</a>"));
            String url = extractBetween(block, "class=\"result__url\" href=\"", "\"");
            if (StrUtil.isNotBlank(title) && StrUtil.isNotBlank(snippet)
                    && StrUtil.isNotBlank(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
                results.add(WebSearchResult.builder()
                        .title(title)
                        .snippet(snippet)
                        .url(url)
                        .source("DuckDuckGo 备用检索")
                        .build());
            }
        }
        return results;
    }

    private String extractBetween(String source, String startTag, String endTag) {
        int start = source.indexOf(startTag);
        if (start < 0) return "";
        start += startTag.length();
        int end = source.indexOf(endTag, start);
        if (end < 0) return "";
        return source.substring(start, end);
    }

    private String cleanHtml(String text) {
        if (StrUtil.isBlank(text)) return "";
        return text.replaceAll("<[^>]*>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private record WebSearchSettings(
            boolean enabled,
            String strategy,
            boolean duckduckgoFallback,
            int maxCitations,
            List<CustomWebSearchEngineConfig> customProviders
    ) {
    }
}
