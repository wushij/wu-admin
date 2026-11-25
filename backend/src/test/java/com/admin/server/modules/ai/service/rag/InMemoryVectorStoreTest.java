package com.admin.server.modules.ai.service.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryVectorStoreTest {

    @Test
    void shouldRankByCosineSimilarity() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        Long kb = 1L;
        store.upsert(kb, 10L, 100L, new float[]{1f, 0f, 0f});
        store.upsert(kb, 10L, 101L, new float[]{0.9f, 0.1f, 0f});
        store.upsert(kb, 11L, 102L, new float[]{0f, 1f, 0f});

        List<VectorStore.VectorHit> hits = store.search(kb, new float[]{1f, 0f, 0f}, 2, 0.1);
        assertEquals(2, hits.size());
        assertEquals(100L, hits.get(0).chunkId());
        assertEquals(101L, hits.get(1).chunkId());
        assertTrue(hits.get(0).score() > hits.get(1).score());
    }

    @Test
    void shouldIgnoreDimensionMismatch() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.upsert(1L, 10L, 100L, new float[]{1f, 0f});
        List<VectorStore.VectorHit> hits = store.search(1L, new float[]{1f, 0f, 0f}, 5, 0);
        assertTrue(hits.isEmpty(), "维度不一致的向量应被跳过，而不是返回错误分数");
    }

    @Test
    void shouldRemoveVectorsByDocument() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.upsert(1L, 10L, 100L, new float[]{1f, 0f});
        store.upsert(1L, 10L, 101L, new float[]{1f, 0f});
        store.upsert(1L, 11L, 102L, new float[]{1f, 0f});
        store.removeByDocument(1L, 10L);
        assertEquals(1, store.size(1L));
        assertEquals(102L, store.search(1L, new float[]{1f, 0f}, 5, 0).get(0).chunkId());
    }

    @Test
    void cosineShouldHandleZeroVector() {
        assertEquals(0, InMemoryVectorStore.cosine(new float[]{0f, 0f}, new float[]{1f, 0f}), 1e-9);
    }
}
