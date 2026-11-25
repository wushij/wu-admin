package com.admin.server.modules.ai.service.rag;

import java.util.List;

/**
 * 向量存储抽象。
 * <p>当前实现为进程内索引（MySQL 保存向量副本用于重启恢复），
 * 保留接口以便后续替换为 Milvus 等外部向量库。</p>
 */
public interface VectorStore {

    /** 新增或覆盖单个切片向量 */
    void upsert(Long knowledgeBaseId, Long documentId, Long chunkId, float[] vector);

    /** 删除单个切片向量 */
    void removeByChunk(Long knowledgeBaseId, Long chunkId);

    /** 删除某文档下的全部向量 */
    void removeByDocument(Long knowledgeBaseId, Long documentId);

    /** 清空某知识库的向量 */
    void clearKnowledgeBase(Long knowledgeBaseId);

    /**
     * 相似度检索。
     *
     * @param knowledgeBaseId 知识库范围
     * @param query           查询向量
     * @param topK            返回条数
     * @param minScore        最小余弦相似度（0~1），低于该值不返回
     */
    List<VectorHit> search(Long knowledgeBaseId, float[] query, int topK, double minScore);

    /** 某知识库已加载的向量数量 */
    int size(Long knowledgeBaseId);

    /** 向量索引是否已完成初始化（启动恢复期间为 false） */
    boolean isReady();

    void markReady();

    /** 检索命中：切片 ID 与余弦相似度 */
    record VectorHit(Long chunkId, double score) {
    }
}
