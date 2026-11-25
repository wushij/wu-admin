package com.admin.server.modules.ai.api.vo;

import com.admin.server.common.core.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgeBasePageReqVO extends PageParam {
    private String name;
    private Integer status;
    private String indexStatus;
}
