package com.admin.server.modules.ai.service.rag;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.ai.api.vo.AiKnowledgeRetrieveRespVO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeBaseDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeChunkDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDocumentDO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeBaseMapper;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeChunkMapper;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeDocumentMapper;
import com.admin.server.modules.ai.providers.EmbeddingClient;
import com.admin.server.modules.ai.service.AiModelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索服务：向量语义召回优先，无模型或空结果时回退关键词检索。
 *
 * <p>供管理端「检索测试」与 AI 对话 RAG 共用，保证两条链路的召回口径一致。</p>
 */
@Service
public class KnowledgeVectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeVectorSearchService.class);

    /** 默认最小余弦相似度 */
    public static final double DEFAULT_MIN_SCORE = 0.25;
    /** 向量召回放大倍数，先多取再过滤，避免边界切片被截断 */
    private static final int RECALL_MULTIPLIER = 2;
    private static final int KEYWORD_FALLBACK_LIMIT = 20;
    /** 单次关键词兜底最多使用的检索词数量（含中文二元组），避免长 query 生成过多 OR LIKE */
    private static final int MAX_SEARCH_TERMS = 12;

    public static final String MATCH_VECTOR = "VECTOR";
    public static final String MATCH_KEYWORD = "KEYWORD";
    public static final String MATCH_NONE = "NONE";

    @Resource
    private AiKnowledgeBaseMapper baseMapper;

    @Resource
    private AiKnowledgeDocumentMapper documentMapper;

    @Resource
    private AiKnowledgeChunkMapper chunkMapper;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private EmbeddingClient embeddingClient;

    @Resource
    private VectorStore vectorStore;

    /** 检索命中 */
    public record ChunkHit(Long chunkId, Long documentId, String documentName,
                           String heading, String content, double score) {
    }

    /** 检索结果（含命中方式，管理端展示用） */
    public record SearchOutcome(List<ChunkHit> hits, String matchMode) {
    }

    /**
     * 单知识库检索（检索测试页）。
     */
    public SearchOutcome searchInBase(Long knowledgeBaseId, String query, int topK, double minScore) {
        AiKnowledgeBaseDO base = baseMapper.selectById(knowledgeBaseId);
        if (base == null) {
            return new SearchOutcome(List.of(), MATCH_NONE);
        }
        List<ChunkHit> vectorHits = vectorSearch(List.of(base), query, topK, minScore);
        if (!vectorHits.isEmpty()) {
            return new SearchOutcome(vectorHits, MATCH_VECTOR);
        }
        List<ChunkHit> keywordHits = keywordSearch(List.of(knowledgeBaseId), query, topK);
        return new SearchOutcome(keywordHits,
                keywordHits.isEmpty() ? MATCH_NONE : MATCH_KEYWORD);
    }

    /**
     * 跨全部启用知识库检索（AI 对话 RAG）。
     */
    public List<ChunkHit> searchAcrossEnabledBases(String query, int topK, double minScore) {
        List<AiKnowledgeBaseDO> bases = baseMapper.selectList(new LambdaQueryWrapper<AiKnowledgeBaseDO>()
                .eq(AiKnowledgeBaseDO::getStatus, 1));
        if (bases.isEmpty() || StrUtil.isBlank(query)) {
            return List.of();
        }
        List<ChunkHit> vectorHits = vectorSearch(bases, query, topK, minScore);
        if (!vectorHits.isEmpty()) {
            return vectorHits;
        }
        List<Long> baseIds = bases.stream().map(AiKnowledgeBaseDO::getId).toList();
        return keywordSearch(baseIds, query, topK);
    }

    /** 向量召回：按知识库各自模型做 embedding（同模型复用查询向量），合并后统一排序 */
    private List<ChunkHit> vectorSearch(List<AiKnowledgeBaseDO> bases, String query, int topK, double minScore) {
        if (StrUtil.isBlank(query) || !vectorStore.isReady()) {
            return List.of();
        }
        int limit = Math.max(1, topK);
        Map<Long, float[]> queryCache = new HashMap<>();
        List<VectorStore.VectorHit> merged = new ArrayList<>();
        for (AiKnowledgeBaseDO base : bases) {
            if (vectorStore.size(base.getId()) == 0) {
                continue;
            }
            try {
                AiModelDO model = aiModelService.getAvailableEmbeddingModel(
                        base.getEmbeddingModelId() == null || base.getEmbeddingModelId() <= 0
                                ? null : base.getEmbeddingModelId());
                float[] queryVector = queryCache.computeIfAbsent(model.getId(),
                        key -> embeddingClient.embed(model, aiModelService.decryptApiKey(model), List.of(query)).get(0));
                List<VectorStore.VectorHit> hits = vectorStore.search(
                        base.getId(), queryVector, limit * RECALL_MULTIPLIER, minScore);
                merged.addAll(hits);
            } catch (Exception e) {
                log.warn("知识库向量检索失败 baseId={}: {}", base.getId(), e.getMessage());
            }
        }
        if (merged.isEmpty()) {
            return List.of();
        }
        merged.sort(Comparator.comparingDouble(VectorStore.VectorHit::score).reversed());
        List<VectorStore.VectorHit> top = merged.size() > limit ? merged.subList(0, limit) : merged;
        return toChunkHits(top);
    }

    /** 关键词兜底：整句与分词结果分别做标题/正文 LIKE，按更新时间倒序 */
    private List<ChunkHit> keywordSearch(List<Long> knowledgeBaseIds, String query, int topK) {
        if (StrUtil.isBlank(query) || knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return List.of();
        }
        List<String> terms = splitTerms(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        List<AiKnowledgeChunkDO> chunks = chunkMapper.selectList(new LambdaQueryWrapper<AiKnowledgeChunkDO>()
                .in(AiKnowledgeChunkDO::getKnowledgeBaseId, knowledgeBaseIds)
                .and(w -> {
                    for (int i = 0; i < terms.size(); i++) {
                        if (i > 0) {
                            w.or();
                        }
                        String term = terms.get(i);
                        w.like(AiKnowledgeChunkDO::getContent, term)
                                .or().like(AiKnowledgeChunkDO::getHeading, term);
                    }
                })
                .orderByDesc(AiKnowledgeChunkDO::getUpdateTime)
                .last("LIMIT " + Math.min(Math.max(topK, 1), KEYWORD_FALLBACK_LIMIT)));
        if (chunks.isEmpty()) {
            return List.of();
        }
        Map<Long, String> documentNames = loadDocumentNames(chunks);
        List<ChunkHit> hits = new ArrayList<>(chunks.size());
        for (AiKnowledgeChunkDO chunk : chunks) {
            hits.add(new ChunkHit(chunk.getId(), chunk.getDocumentId(),
                    documentNames.getOrDefault(chunk.getDocumentId(), ""),
                    StrUtil.nullToEmpty(chunk.getHeading()),
                    StrUtil.nullToEmpty(chunk.getContent()), 0));
        }
        return hits;
    }

    /**
     * 提取检索词：整句、标点切分片段，中文长片段再补充二元组。
     * <p>中文没有空格，仅按标点切分无法得到「忘记密码」这类子串，
     * 由二元组（相邻两字）保证 LIKE 兜底能命中自然语言提问。</p>
     */
    private List<String> splitTerms(String query) {
        java.util.LinkedHashSet<String> terms = new java.util.LinkedHashSet<>();
        String trimmed = StrUtil.brief(query.trim(), 60);
        if (StrUtil.isBlank(trimmed)) {
            return List.of();
        }
        terms.add(trimmed);
        for (String part : trimmed.split("[\\s,，、;；:：?？!！.。\"'“”‘’()（）\\[\\]【】]+")) {
            String term = part.trim();
            if (term.length() < 2) {
                continue;
            }
            terms.add(term);
            if (term.length() >= 4) {
                for (int i = 0; i + 2 <= term.length() && terms.size() < MAX_SEARCH_TERMS; i++) {
                    String bigram = term.substring(i, i + 2);
                    if (bigram.codePoints().allMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN)) {
                        terms.add(bigram);
                    }
                }
            }
        }
        return terms.stream().limit(MAX_SEARCH_TERMS).toList();
    }

    private List<ChunkHit> toChunkHits(List<VectorStore.VectorHit> vectorHits) {
        if (vectorHits.isEmpty()) {
            return List.of();
        }
        List<Long> chunkIds = vectorHits.stream().map(VectorStore.VectorHit::chunkId).toList();
        List<AiKnowledgeChunkDO> chunks = chunkMapper.selectBatchIds(chunkIds);
        Map<Long, AiKnowledgeChunkDO> chunkMap = new HashMap<>();
        for (AiKnowledgeChunkDO chunk : chunks) {
            chunkMap.put(chunk.getId(), chunk);
        }
        Map<Long, String> documentNames = loadDocumentNames(chunks);
        List<ChunkHit> hits = new ArrayList<>(vectorHits.size());
        for (VectorStore.VectorHit hit : vectorHits) {
            AiKnowledgeChunkDO chunk = chunkMap.get(hit.chunkId());
            if (chunk == null) {
                continue;
            }
            hits.add(new ChunkHit(chunk.getId(), chunk.getDocumentId(),
                    documentNames.getOrDefault(chunk.getDocumentId(), ""),
                    StrUtil.nullToEmpty(chunk.getHeading()),
                    StrUtil.nullToEmpty(chunk.getContent()), hit.score()));
        }
        return hits;
    }

    private Map<Long, String> loadDocumentNames(List<AiKnowledgeChunkDO> chunks) {
        List<Long> documentIds = chunks.stream().map(AiKnowledgeChunkDO::getDocumentId).distinct().toList();
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        List<AiKnowledgeDocumentDO> documents = documentMapper.selectBatchIds(documentIds);
        Map<Long, String> names = new HashMap<>();
        for (AiKnowledgeDocumentDO document : documents) {
            names.put(document.getId(), StrUtil.nullToEmpty(document.getFileName()));
        }
        return names;
    }

    /** 管理端检索测试出参组装 */
    public AiKnowledgeRetrieveRespVO toResp(Long knowledgeBaseId, String query, int topK, double minScore) {
        SearchOutcome outcome = searchInBase(knowledgeBaseId, query, topK, minScore);
        AiKnowledgeRetrieveRespVO resp = new AiKnowledgeRetrieveRespVO();
        resp.setQuery(query);
        resp.setMatchMode(outcome.matchMode());
        resp.setTopK(topK);
        resp.setMinScore(minScore);
        List<AiKnowledgeRetrieveRespVO.Item> items = new ArrayList<>(outcome.hits().size());
        for (ChunkHit hit : outcome.hits()) {
            AiKnowledgeRetrieveRespVO.Item item = new AiKnowledgeRetrieveRespVO.Item();
            item.setChunkId(hit.chunkId());
            item.setDocumentId(hit.documentId());
            item.setDocumentName(hit.documentName());
            item.setHeading(hit.heading());
            item.setContent(hit.content());
            item.setScore(hit.score());
            items.add(item);
        }
        resp.setItems(items);
        return resp;
    }
}
