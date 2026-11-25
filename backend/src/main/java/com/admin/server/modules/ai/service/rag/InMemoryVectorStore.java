package com.admin.server.modules.ai.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内向量索引：按知识库分桶的 map + 余弦相似度线性扫描。
 *
 * <p>适用规模为十万级切片；查询按知识库隔离，避免跨库串味。
 * 向量副本持久化在 {@code sys_ai_knowledge_chunk.embedding_vector}，
 * 启动时由 {@code VectorIndexBootstrap} 恢复。</p>
 */
@Component
public class InMemoryVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorStore.class);

    /** knowledgeBaseId -> (chunkId -> entry) */
    private final Map<Long, Map<Long, VectorEntry>> buckets = new ConcurrentHashMap<>();

    private volatile boolean ready = true;

    @Override
    public void upsert(Long knowledgeBaseId, Long documentId, Long chunkId, float[] vector) {
        if (knowledgeBaseId == null || chunkId == null || vector == null || vector.length == 0) {
            return;
        }
        buckets.computeIfAbsent(knowledgeBaseId, key -> new ConcurrentHashMap<>())
                .put(chunkId, new VectorEntry(documentId, vector));
    }

    @Override
    public void removeByChunk(Long knowledgeBaseId, Long chunkId) {
        Map<Long, VectorEntry> bucket = buckets.get(knowledgeBaseId);
        if (bucket != null && chunkId != null) {
            bucket.remove(chunkId);
        }
    }

    @Override
    public void removeByDocument(Long knowledgeBaseId, Long documentId) {
        Map<Long, VectorEntry> bucket = buckets.get(knowledgeBaseId);
        if (bucket == null || documentId == null) {
            return;
        }
        bucket.entrySet().removeIf(entry -> documentId.equals(entry.getValue().documentId()));
    }

    @Override
    public void clearKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId != null) {
            buckets.remove(knowledgeBaseId);
        }
    }

    @Override
    public List<VectorHit> search(Long knowledgeBaseId, float[] query, int topK, double minScore) {
        Map<Long, VectorEntry> bucket = buckets.get(knowledgeBaseId);
        if (bucket == null || bucket.isEmpty() || query == null || query.length == 0) {
            return List.of();
        }
        int limit = Math.max(1, topK);
        List<VectorHit> hits = new ArrayList<>();
        for (Map.Entry<Long, VectorEntry> entry : bucket.entrySet()) {
            float[] vector = entry.getValue().vector();
            if (vector.length != query.length) {
                // 维度不一致说明模型配置变更后未重建索引，跳过而不是返回错误分数
                continue;
            }
            double score = cosine(query, vector);
            if (score >= minScore) {
                hits.add(new VectorHit(entry.getKey(), score));
            }
        }
        hits.sort(Comparator.comparingDouble(VectorHit::score).reversed());
        return hits.size() > limit ? new ArrayList<>(hits.subList(0, limit)) : hits;
    }

    @Override
    public int size(Long knowledgeBaseId) {
        Map<Long, VectorEntry> bucket = buckets.get(knowledgeBaseId);
        return bucket == null ? 0 : bucket.size();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void markReady() {
        this.ready = true;
        int total = buckets.values().stream().mapToInt(Map::size).sum();
        log.info("知识库向量索引初始化完成，已加载 {} 个知识库、{} 条向量", buckets.size(), total);
    }

    /** 启动恢复期间标记为未就绪，检索请求会给出明确提示而不是返回空结果 */
    public void markLoading() {
        this.ready = false;
    }

    /** 余弦相似度；零向量返回 0，避免除零 */
    static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record VectorEntry(Long documentId, float[] vector) {
    }
}
