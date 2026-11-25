package com.admin.server.modules.ai.api.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库检索测试入参
 */
@Data
public class AiKnowledgeRetrieveReqVO {
    @NotNull(message = "知识库不能为空")
    private Long knowledgeBaseId;
    @NotBlank(message = "检索问题不能为空")
    private String query;
    /** 返回条数，默认 5，范围 1~20 */
    private Integer topK;
    /** 最小余弦相似度，默认 0.2 */
    private Double minScore;
}
