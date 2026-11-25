package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库切片出参
 */
@Data
public class AiKnowledgeChunkRespVO {
    private Long id;
    private Long knowledgeBaseId;
    private Long documentId;
    /** 所属文档名（列表展示用） */
    private String documentName;
    private Integer chunkIndex;
    private String heading;
    private String content;
    private Integer charCount;
    private Integer tokenEstimate;
    /** 向量状态：PENDING/INDEXED/FAILED */
    private String embedStatus;
    private String embeddingModel;
    private Integer embeddingDim;
    private String errorMsg;
    private LocalDateTime createTime;
}
