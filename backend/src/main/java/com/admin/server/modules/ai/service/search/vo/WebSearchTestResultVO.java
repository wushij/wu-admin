package com.admin.server.modules.ai.service.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 自定义联网搜索引擎连通性测试返回结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSearchTestResultVO {

    /** 是否试搜成功 */
    private boolean success;

    /** 引擎名称 */
    private String engineName;

    /** 耗时（毫秒） */
    private long latencyMs;

    /** 诊断说明 */
    private String message;

    /** 命中样本列表 */
    private List<WebSearchResult> results;
}
