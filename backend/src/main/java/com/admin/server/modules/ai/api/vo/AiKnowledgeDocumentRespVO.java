package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库文档出参
 */
@Data
public class AiKnowledgeDocumentRespVO {
    private Long id;
    private Long knowledgeBaseId;
    private Long fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer charCount;
    private Integer chunkCount;
    /** 解析状态：PENDING/PARSING/SUCCESS/FAILED */
    private String parseStatus;
    /** 向量状态：PENDING/INDEXING/INDEXED/PARTIAL/FAILED */
    private String indexStatus;
    private String errorMsg;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
