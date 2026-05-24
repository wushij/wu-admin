package com.admin.server.modules.ai.api.vo;

import com.admin.server.common.core.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgePageReqVO extends PageParam {
    /** 标题/关键词模糊匹配 */
    private String keyword;
    private String category;
    private Integer status;
}
