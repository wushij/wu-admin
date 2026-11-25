package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库出参
 */
@Data
public class AiKnowledgeBaseRespVO {
    private Long id;
    private String name;
    private String description;
    private Long embeddingModelId;
    /** 实际生效的向量模型名称（未单独配置时为默认向量模型） */
    private String embeddingModelName;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer documentCount;
    private Integer chunkCount;
    /** 已向量化切片数 */
    private Integer indexedChunkCount;
    private String indexStatus;
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
