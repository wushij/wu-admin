package com.admin.server.modules.ai.service.search;

import com.admin.server.modules.ai.service.search.vo.WebSearchResult;

import java.util.List;

/**
 * 联网搜索能力统一门面接口
 */
public interface WebSearchService {

    /**
     * 执行多引擎联网搜索
     *
     * @param query 检索关键词
     * @param limit 最大返回条数
     * @return 标准化联网检索结果列表（失败或未启用时返回空列表）
     */
    List<WebSearchResult> search(String query, int limit);

    /**
     * 执行多引擎联网搜索（支持调用方或大模型指定时效窗口：day / week / month / none）
     */
    default List<WebSearchResult> search(String query, int limit, String preferredTimeRange) {
        return search(query, limit);
    }

    /**
     * 判断后台是否启用了联网搜索能力（总开关开启且存在至少一个可用引擎或开启了免 Key 兜底）
     */
    boolean isCapabilityEnabled();

    /**
     * 获取当前启用的联网引擎摘要文案（如 "DeepSeek 联网搜索 · Tavily"）
     */
    String resolveActiveSummary();

    /**
     * 将搜索结果格式化为系统提示词参考资料块
     */
    String formatSearchResultsForPrompt(List<WebSearchResult> results);

    /**
     * 当所有联网检索源均无结果时的兜底提示词
     */
    String buildFallbackPrompt(String query);
}
