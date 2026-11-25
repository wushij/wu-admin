package com.admin.server.modules.ai.api.vo;

import com.admin.server.common.core.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库切片分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgeChunkPageReqVO extends PageParam {
    private Long knowledgeBaseId;
    private Long documentId;
    /** 关键词：匹配切片正文与标题 */
    private String keyword;
    /** 向量状态：PENDING/INDEXED/FAILED */
    private String embedStatus;
}
