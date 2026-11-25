package com.admin.server.modules.ai.service.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tavily 官方账户额度与连通性视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TavilyUsageVO {

    /** 密钥是否有效且连通成功 */
    private boolean valid;

    /** 状态描述文案 */
    private String message;

    /** 当月已使用总次数 */
    private Integer usage;

    /** 当月总配额上限 */
    private Integer limit;

    /** 当月剩余可用次数 */
    private Integer remaining;

    /** 已消耗比例（0~100） */
    private Double percent;

    /** 当前套餐名称 */
    private String currentPlan;

    /** 搜索调用次数 */
    private Integer searchUsage;

    /** 站点爬取调用次数 */
    private Integer crawlUsage;

    /** 正文提取调用次数 */
    private Integer extractUsage;

    /** 站点地图调用次数 */
    private Integer mapUsage;

    /** 深度研究调用次数 */
    private Integer researchUsage;

    /** 超额按量次数 */
    private Integer paygoUsage;

    /** 按量补充额度上限 */
    private Integer paygoLimit;
}
