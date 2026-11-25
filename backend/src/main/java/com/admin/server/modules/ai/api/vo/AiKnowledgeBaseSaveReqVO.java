package com.admin.server.modules.ai.api.vo;

import lombok.Data;

/**
 * AI 知识库新增/编辑入参
 */
@Data
public class AiKnowledgeBaseSaveReqVO {
    private Long id;
    private String name;
    private String description;
    /** 向量模型 ID，0 或空表示跟随默认向量模型 */
    private Long embeddingModelId;
    /** 切片目标字符数，留空默认 800 */
    private Integer chunkSize;
    /** 切片重叠字符数，留空默认 128 */
    private Integer chunkOverlap;
    private Integer status;
    private Integer sort;
}
