package com.admin.server.modules.ai.tool.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.admin.server.modules.ai.service.search.WebSearchService;
import com.admin.server.modules.ai.service.search.vo.WebSearchCitationVO;
import com.admin.server.modules.ai.service.search.vo.WebSearchResult;
import com.admin.server.modules.ai.tool.AiTool;
import com.admin.server.modules.ai.tool.AiToolContext;
import com.admin.server.modules.ai.tool.ToolAccessLevel;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 自主联网搜索工具（USER 级，受用户前端联网开关与系统联网总配置双重门控）
 * <p>
 * 当用户开启联网开关且提问需要实时外部资料（天气、新闻、版本发布、公开价格等）时，
 * 由大模型自主提炼精准检索关键词调用此工具，并将命中外链同步写入上下文供前端展示溯源卡片。
 * </p>
 */
@Component
public class WebSearchTool implements AiTool {

    private static final int DEFAULT_LIMIT = 5;
    private static final long WEB_SEARCH_TIMEOUT_MS = 18_000L;

    @Resource
    private WebSearchService webSearchService;

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "实时联网检索全网最新公开资料。当用户询问天气、新闻、最新产品版本/价格、时事动态等需要联网查询的问题，且本地知识库未覆盖时调用；"
                + "请先自行提炼简洁精准的搜索关键词（例如「佛山 实时天气预报」「Spring Boot 最新版本」），"
                + "切勿把「怎么样/请问/帮我查」等整句口语提问当作关键词，也不要无端硬拼日期字符串。";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject queryProp = new JSONObject()
                .set("type", "string")
                .set("description", "精炼的检索关键词（建议 2~6 个核心词，如「佛山 实时天气预报」或「DeepSeek API 定价」）");
        JSONObject timeRangeProp = new JSONObject()
                .set("type", "string")
                .set("enum", new JSONArray().set("day").set("week").set("month").set("none"))
                .set("description", "可选时效过滤：day 仅严格今日突发新闻；week 适合天气、近期更新与本周动态；month 近一月；none 不限（查普通天气建议优先 week 或不传）");
        JSONObject props = new JSONObject()
                .set("query", queryProp)
                .set("timeRange", timeRangeProp);
        return new JSONObject()
                .set("type", "object")
                .set("properties", props)
                .set("required", new JSONArray().set("query"));
    }

    @Override
    public ToolAccessLevel accessLevel() {
        return ToolAccessLevel.USER;
    }

    @Override
    public String requiredPermission() {
        return null;
    }

    @Override
    public boolean isAvailable(AiToolContext ctx) {
        return ctx != null && ctx.isWebSearchEnabled() && webSearchService.isCapabilityEnabled();
    }

    @Override
    public long timeoutMs() {
        return WEB_SEARCH_TIMEOUT_MS;
    }

    @Override
    public String execute(JSONObject args, AiToolContext ctx) {
        String query = args != null ? StrUtil.trimToEmpty(args.getStr("query")) : "";
        if (StrUtil.isBlank(query)) {
            return new JSONObject()
                    .set("ok", false)
                    .set("error", "搜索关键词 query 不能为空")
                    .toString();
        }
        String timeRange = args != null ? StrUtil.trim(args.getStr("timeRange")) : null;
        List<WebSearchResult> results = webSearchService.search(query, DEFAULT_LIMIT, timeRange);
        if (results == null || results.isEmpty()) {
            return new JSONObject()
                    .set("ok", true)
                    .set("query", query)
                    .set("count", 0)
                    .set("results", new JSONArray())
                    .set("hint", "本次检索未命中有效网页结果，可尝试更换更简洁的关键词或使用英文关键词再搜一次。")
                    .toString();
        }

        List<WebSearchCitationVO> citations = new ArrayList<>();
        JSONArray itemsJson = new JSONArray();
        for (WebSearchResult sr : results) {
            if (sr == null || StrUtil.isBlank(sr.getTitle())) {
                continue;
            }
            String url = StrUtil.trimToEmpty(sr.getUrl());
            if (!isPublicHttpCitation(url)) {
                continue;
            }
            String snippet = StrUtil.nullToEmpty(sr.getSnippet());
            String source = StrUtil.blankToDefault(sr.getSource(), "联网检索");
            citations.add(WebSearchCitationVO.builder()
                    .title(sr.getTitle())
                    .url(url)
                    .snippet(StrUtil.brief(snippet, 200))
                    .source(source)
                    .build());
            itemsJson.add(new JSONObject()
                    .set("title", sr.getTitle())
                    .set("source", source)
                    .set("url", url)
                    .set("snippet", StrUtil.brief(snippet, 600)));
        }
        if (ctx != null && !citations.isEmpty()) {
            ctx.addCitations(citations);
        }
        return new JSONObject()
                .set("ok", true)
                .set("query", query)
                .set("count", itemsJson.size())
                .set("results", itemsJson)
                .toString();
    }

    private boolean isPublicHttpCitation(String url) {
        return (url.startsWith("http://") || url.startsWith("https://"))
                && !url.toLowerCase().contains("tavily.com");
    }
}
