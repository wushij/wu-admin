package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.util.List;

/**
 * 知识库检索测试出参
 */
@Data
public class AiKnowledgeRetrieveRespVO {
    private String query;
    /** 命中方式：VECTOR=向量检索 / KEYWORD=关键词兜底 / NONE=无命中 */
    private String matchMode;
    private Integer topK;
    private Double minScore;
    private List<Item> items;

    @Data
    public static class Item {
        private Long chunkId;
        private Long documentId;
        private String documentName;
        private String heading;
        private String content;
        /** 余弦相似度（关键词兜底时为 0） */
        private Double score;
    }
}
