package com.admin.server.modules.ai.service.rag;

import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeChunkDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeChunkMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时从 MySQL 向量副本恢复进程内索引。
 *
 * <p>恢复期间 {@link VectorStore#isReady()} 为 false，检索会跳过向量分支并回退关键词，
 * 不会返回错误的空结果。单次级为 500 条、游标分页，避免一次性加载大表。</p>
 */
@Component
public class VectorIndexBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexBootstrap.class);

    private static final int PAGE_SIZE = 500;
    /** 单次启动最多恢复的切片数，超出部分提示手动重建，防止启动时间不可控 */
    private static final int MAX_RESTORE = 100_000;

    @Resource
    private AiKnowledgeChunkMapper chunkMapper;

    @Resource
    private VectorStore vectorStore;

    @Override
    public void run(ApplicationArguments args) {
        long start = System.currentTimeMillis();
        if (vectorStore instanceof InMemoryVectorStore inMemoryStore) {
            inMemoryStore.markLoading();
        }
        int restored = 0;
        long cursor = 0;
        try {
            while (restored < MAX_RESTORE) {
                List<AiKnowledgeChunkDO> batch = chunkMapper.selectIndexedAfter(cursor, PAGE_SIZE);
                if (batch.isEmpty()) {
                    break;
                }
                for (AiKnowledgeChunkDO chunk : batch) {
                    cursor = chunk.getId();
                    float[] vector = KnowledgeIndexService.parseVector(chunk.getEmbeddingVector());
                    if (vector.length == 0) {
                        continue;
                    }
                    vectorStore.upsert(chunk.getKnowledgeBaseId(), chunk.getDocumentId(), chunk.getId(), vector);
                    restored++;
                }
                if (batch.size() < PAGE_SIZE) {
                    break;
                }
            }
            vectorStore.markReady();
            log.info("知识库向量索引恢复完成：{} 条，耗时 {}ms", restored, System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 恢复失败不阻断启动：标记就绪但为空，检索自动回退关键词，管理员可手动重建
            vectorStore.markReady();
            log.error("知识库向量索引恢复失败，检索将回退关键词模式: {}", e.getMessage());
        }
    }
}
