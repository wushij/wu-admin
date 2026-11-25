package com.admin.server.modules.ai.api.vo;

import lombok.Data;

/**
 * 知识库切片统计（详情页指标卡）
 */
@Data
public class AiKnowledgeChunkStatsVO {
    /** 切片总数 */
    private Long total;
    /** 已向量化 */
    private Long indexed;
    /** 待向量化 */
    private Long pending;
    /** 向量化失败 */
    private Long failed;
    /** 单切片平均 Token（估算） */
    private Long avgTokens;
    /** 全部切片 Token 总量（估算） */
    private Long totalTokens;
}
