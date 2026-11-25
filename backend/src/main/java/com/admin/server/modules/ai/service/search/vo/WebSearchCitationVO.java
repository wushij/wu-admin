package com.admin.server.modules.ai.service.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下发给前端 AI 助手气泡的联网引用溯源项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSearchCitationVO {

    /** 网页标题 */
    private String title;

    /** 真实可访问的网页链接（http/https） */
    private String url;

    /** 摘录内容 */
    private String snippet;

    /** 引擎来源显示名称 */
    private String source;
}
