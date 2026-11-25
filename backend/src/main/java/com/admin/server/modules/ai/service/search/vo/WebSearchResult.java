package com.admin.server.modules.ai.service.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联网检索单条标准化结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSearchResult {

    /** 网页标题 */
    private String title;

    /** 网页摘要/正文切片 */
    private String snippet;

    /** 网页原始链接（仅限 http/https） */
    private String url;

    /** 检索来源名称（如 Tavily / DeepSeek 联网 / 博查搜索） */
    private String source;

    /** 相关性评分（可选） */
    private Double score;
}
