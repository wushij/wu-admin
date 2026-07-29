package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库出参
 */
@Data
public class AiKnowledgeRespVO {
    private Long id;
    private String title;
    private String keywords;
    private String content;
    private String category;
    private Integer sort;
    private Integer status;
    private String creator;
    private String updater;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
