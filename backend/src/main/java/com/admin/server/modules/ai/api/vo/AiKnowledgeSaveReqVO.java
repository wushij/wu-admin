package com.admin.server.modules.ai.api.vo;

import lombok.Data;

/**
 * AI 知识库新增/编辑入参
 */
@Data
public class AiKnowledgeSaveReqVO {
    private Long id;
    private String title;
    private String keywords;
    private String content;
    private String category;
    private Integer sort;
    private Integer status;
}
