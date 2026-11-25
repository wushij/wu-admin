package com.admin.server.modules.ai.api.vo;

import com.admin.server.common.core.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库文档分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgeDocumentPageReqVO extends PageParam {
    private Long knowledgeBaseId;
    private String fileName;
    private String parseStatus;
    private String indexStatus;
}
