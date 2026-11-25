package com.admin.server.modules.ai.service.search;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.ai.service.search.vo.WebSearchCitationVO;
import com.admin.server.modules.ai.service.search.vo.WebSearchResult;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 联网检索上下文与参考溯源构建器
 * <p>
 * 负责将真实检索命中的全网资料注入系统提示词，并产出可安全展示给前端的引用卡片（仅保留真实 http/https 外链）。
 * </p>
 */
@Component
public class WebSearchContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(WebSearchContextBuilder.class);

    @Resource
    private WebSearchService webSearchService;

    /**
     * 执行联网搜索并将上下文注入 systemPrompt，同时填充有效外链引用
     */
    public String enrichWithWebSearch(String query, int limit, String systemPrompt, List<WebSearchCitationVO> citations) {
        if (StrUtil.isBlank(query)) {
            return systemPrompt;
        }
        String basePrompt = StrUtil.nullToEmpty(systemPrompt);
        try {
            List<WebSearchResult> searchResults = webSearchService.search(query, limit);
            if (searchResults != null && !searchResults.isEmpty()) {
                String block = webSearchService.formatSearchResultsForPrompt(searchResults);
                if (StrUtil.isNotBlank(block)) {
                    basePrompt = basePrompt + "\n" + block;
                }
                if (citations != null) {
                    for (WebSearchResult sr : searchResults) {
                        if (sr == null || StrUtil.isBlank(sr.getTitle())) {
                            continue;
                        }
                        String url = StrUtil.trimToEmpty(sr.getUrl());
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            continue;
                        }
                        if (url.toLowerCase().contains("tavily.com")) {
                            continue;
                        }
                        citations.add(WebSearchCitationVO.builder()
                                .title(sr.getTitle())
                                .url(url)
                                .snippet(StrUtil.brief(StrUtil.nullToEmpty(sr.getSnippet()), 200))
                                .source(StrUtil.blankToDefault(sr.getSource(), "联网检索"))
                                .build());
                    }
                }
                return basePrompt;
            }
            String fallbackPrompt = webSearchService.buildFallbackPrompt(query);
            if (StrUtil.isNotBlank(fallbackPrompt)) {
                return basePrompt + "\n" + fallbackPrompt;
            }
        } catch (Exception ex) {
            log.warn("联网检索增强异常（降级为常规问答）: query={}, error={}", query, ex.getMessage());
        }
        return basePrompt;
    }
}
