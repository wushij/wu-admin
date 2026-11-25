package com.admin.server.modules.ai.service.rag;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeBaseDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeChunkDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDocumentDO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeBaseMapper;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeChunkMapper;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeDocumentMapper;
import com.admin.server.modules.ai.providers.EmbeddingClient;
import com.admin.server.modules.ai.service.AiKnowledgeBaseService;
import com.admin.server.modules.ai.service.AiModelService;
import com.admin.server.modules.infra.service.file.SysFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库索引编排：解析 → 切片 → 向量化。
 *
 * <p>每个阶段独立落库状态，失败可重试；整条链路在专用线程池中执行，
 * 不占用 Web 请求线程。未配置向量模型时切片照常生成，仅保留 PENDING 状态，
 * 检索端会自动回退关键词检索。</p>
 */
@Service
public class KnowledgeIndexService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexService.class);

    private static final String PARSE_PENDING = "PENDING";
    private static final String PARSE_PARSING = "PARSING";
    private static final String PARSE_SUCCESS = "SUCCESS";
    private static final String PARSE_FAILED = "FAILED";

    private static final String EMBED_PENDING = "PENDING";
    private static final String EMBED_INDEXED = "INDEXED";
    private static final String EMBED_FAILED = "FAILED";

    private static final String INDEX_PENDING = "PENDING";
    private static final String INDEX_INDEXING = "INDEXING";
    private static final String INDEX_INDEXED = "INDEXED";
    private static final String INDEX_PARTIAL = "PARTIAL";
    private static final String INDEX_FAILED = "FAILED";

    private static final int ERROR_MSG_MAX = 480;

    @Resource
    private AiKnowledgeDocumentMapper documentMapper;

    @Resource
    private AiKnowledgeChunkMapper chunkMapper;

    @Resource
    private AiKnowledgeBaseMapper baseMapper;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private EmbeddingClient embeddingClient;

    @Resource
    private DocumentTextExtractor documentTextExtractor;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private SysFileService sysFileService;

    /** 延迟注入，避免 BaseService → IndexService → BaseService 的构造期循环 */
    @Lazy
    @Resource
    private AiKnowledgeBaseService knowledgeBaseService;

    /**
     * 文档全流程：解析正文 → 切片 → 向量化。
     */
    @Async(AiKnowledgeAsyncConfig.EXECUTOR_NAME)
    public void processDocument(Long documentId) {
        AiKnowledgeDocumentDO document = documentMapper.selectById(documentId);
        if (document == null) {
            return;
        }
        AiKnowledgeBaseDO base = baseMapper.selectById(document.getKnowledgeBaseId());
        if (base == null) {
            return;
        }
        try {
            updateDocument(document.getId(), PARSE_PARSING, null, null);
            String content = document.getContent();
            if (StrUtil.isBlank(content) && document.getFileId() != null && document.getFileId() > 0) {
                // 异步线程没有登录上下文，必须走内部读取，避免数据范围校验误拦截
                byte[] bytes = sysFileService.getFileBytesInternal(document.getFileId());
                content = documentTextExtractor.extract(bytes, document.getFileName());
            }
            if (StrUtil.isBlank(content)) {
                throw new IllegalStateException("文档正文为空，请重新上传或检查文件内容");
            }
            document.setContent(content);
            updateDocument(document.getId(), PARSE_SUCCESS, content, (int) content.length());
            chunkAndVectorize(document, base);
        } catch (Exception e) {
            log.warn("知识文档处理失败 documentId={}: {}", documentId, e.getMessage());
            updateDocument(document.getId(), PARSE_FAILED, null, null, e.getMessage());
            updateDocumentIndexStatus(document.getId(), INDEX_FAILED, e.getMessage());
            knowledgeBaseService.refreshStats(document.getKnowledgeBaseId());
        }
    }

    /**
     * 重新切片并向量化（正文已存在，不做解析）。
     */
    @Async(AiKnowledgeAsyncConfig.EXECUTOR_NAME)
    public void reindexDocument(Long documentId) {
        AiKnowledgeDocumentDO document = documentMapper.selectById(documentId);
        if (document == null || StrUtil.isBlank(document.getContent())) {
            return;
        }
        AiKnowledgeBaseDO base = baseMapper.selectById(document.getKnowledgeBaseId());
        if (base == null) {
            return;
        }
        try {
            chunkAndVectorize(document, base);
        } catch (Exception e) {
            log.warn("知识文档重建切片失败 documentId={}: {}", documentId, e.getMessage());
            updateDocumentIndexStatus(document.getId(), INDEX_FAILED, e.getMessage());
            knowledgeBaseService.refreshStats(document.getKnowledgeBaseId());
        }
    }

    /**
     * 全库重建：逐个文档重新切片并向量化（同一个异步任务内串行执行，避免放大并发）。
     */
    @Async(AiKnowledgeAsyncConfig.EXECUTOR_NAME)
    public void reindexKnowledgeBase(Long knowledgeBaseId) {
        AiKnowledgeBaseDO base = baseMapper.selectById(knowledgeBaseId);
        if (base == null) {
            return;
        }
        List<AiKnowledgeDocumentDO> documents = documentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocumentDO>()
                        .eq(AiKnowledgeDocumentDO::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(AiKnowledgeDocumentDO::getStatus, 1));
        for (AiKnowledgeDocumentDO document : documents) {
            if (StrUtil.isBlank(document.getContent())) {
                continue;
            }
            try {
                chunkAndVectorize(document, base);
            } catch (Exception e) {
                log.warn("知识库重建时文档处理失败 documentId={}: {}", document.getId(), e.getMessage());
                updateDocumentIndexStatus(document.getId(), INDEX_FAILED, e.getMessage());
            }
        }
        knowledgeBaseService.refreshStats(knowledgeBaseId);
    }

    /**
     * 重建单个切片的向量（切片页「重建向量」按钮，同步执行，结果立即可见）。
     */
    public void reindexChunk(Long chunkId) {
        AiKnowledgeChunkDO chunk = chunkMapper.selectById(chunkId);
        if (chunk == null) {
            return;
        }
        AiKnowledgeBaseDO base = baseMapper.selectById(chunk.getKnowledgeBaseId());
        if (base == null) {
            return;
        }
        AiModelDO model;
        String apiKey;
        try {
            model = resolveEmbeddingModel(base);
            apiKey = aiModelService.decryptApiKey(model);
        } catch (Exception e) {
            markChunkFailed(chunk, e.getMessage());
            throw e;
        }
        try {
            List<float[]> vectors = embeddingClient.embed(model, apiKey, List.of(chunk.getContent()));
            float[] vector = vectors.get(0);
            markChunkIndexed(chunk, model, vector);
            vectorStore.upsert(chunk.getKnowledgeBaseId(), chunk.getDocumentId(), chunk.getId(), vector);
        } catch (Exception e) {
            markChunkFailed(chunk, e.getMessage());
            throw e;
        }
    }

    /** 切片 + 向量化主流程 */
    private void chunkAndVectorize(AiKnowledgeDocumentDO document, AiKnowledgeBaseDO base) {
        List<MarkdownChunkSplitter.Chunk> pieces = new MarkdownChunkSplitter(
                base.getChunkSize() == null ? 800 : base.getChunkSize(),
                base.getChunkOverlap() == null ? 128 : base.getChunkOverlap())
                .split(document.getContent());
        if (pieces.isEmpty()) {
            updateDocumentIndexStatus(document.getId(), INDEX_FAILED, "未切分出有效切片");
            knowledgeBaseService.refreshStats(base.getId());
            return;
        }

        updateDocumentIndexStatus(document.getId(), INDEX_INDEXING, null);
        // 重建前先清理旧切片与旧向量，保证序号与检索结果不重复
        removeChunksByDocument(document.getId());
        vectorStore.removeByDocument(document.getKnowledgeBaseId(), document.getId());

        List<AiKnowledgeChunkDO> chunks = new ArrayList<>(pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            MarkdownChunkSplitter.Chunk piece = pieces.get(i);
            AiKnowledgeChunkDO chunk = new AiKnowledgeChunkDO();
            chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setHeading(StrUtil.brief(piece.heading(), 250));
            chunk.setContent(piece.content());
            chunk.setCharCount(piece.charCount());
            chunk.setTokenEstimate(piece.tokenEstimate());
            chunk.setEmbedStatus(EMBED_PENDING);
            chunk.setEmbeddingModel("");
            chunk.setEmbeddingDim(0);
            chunk.setErrorMsg("");
            chunkMapper.insert(chunk);
            chunks.add(chunk);
        }
        documentMapper.update(null, new LambdaUpdateWrapper<AiKnowledgeDocumentDO>()
                .eq(AiKnowledgeDocumentDO::getId, document.getId())
                .set(AiKnowledgeDocumentDO::getChunkCount, chunks.size()));

        AiModelDO model = null;
        String apiKey = "";
        try {
            model = resolveEmbeddingModel(base);
            apiKey = aiModelService.decryptApiKey(model);
        } catch (Exception e) {
            // 未配置向量模型：保留切片，等待管理员配置后重建索引
            updateDocumentIndexStatus(document.getId(), INDEX_PENDING, e.getMessage());
            knowledgeBaseService.refreshStats(base.getId());
            return;
        }

        int indexed = 0;
        int failed = 0;
        int batchSize = model.getBatchSize() == null || model.getBatchSize() <= 0 ? 16 : model.getBatchSize();
        for (int from = 0; from < chunks.size(); from += batchSize) {
            int to = Math.min(from + batchSize, chunks.size());
            List<AiKnowledgeChunkDO> batch = chunks.subList(from, to);
            List<String> texts = batch.stream().map(AiKnowledgeChunkDO::getContent).toList();
            try {
                List<float[]> vectors = embeddingClient.embed(model, apiKey, texts);
                for (int i = 0; i < batch.size(); i++) {
                    AiKnowledgeChunkDO chunk = batch.get(i);
                    float[] vector = vectors.get(i);
                    markChunkIndexed(chunk, model, vector);
                    vectorStore.upsert(chunk.getKnowledgeBaseId(), chunk.getDocumentId(), chunk.getId(), vector);
                    indexed++;
                }
            } catch (Exception e) {
                log.warn("知识切片向量化失败 documentId={} range={}-{}: {}",
                        document.getId(), from, to, e.getMessage());
                for (AiKnowledgeChunkDO chunk : batch) {
                    markChunkFailed(chunk, e.getMessage());
                    failed++;
                }
            }
        }

        String indexStatus;
        if (indexed == 0) {
            indexStatus = INDEX_FAILED;
        } else if (failed > 0) {
            indexStatus = INDEX_PARTIAL;
        } else {
            indexStatus = INDEX_INDEXED;
        }
        updateDocumentIndexStatus(document.getId(), indexStatus, failed > 0 ? "部分切片向量化失败，可重试" : null);
        knowledgeBaseService.refreshStats(base.getId());
    }

    private AiModelDO resolveEmbeddingModel(AiKnowledgeBaseDO base) {
        Long modelId = base.getEmbeddingModelId();
        return aiModelService.getAvailableEmbeddingModel(modelId == null || modelId <= 0 ? null : modelId);
    }

    private void removeChunksByDocument(Long documentId) {
        chunkMapper.delete(new LambdaQueryWrapper<AiKnowledgeChunkDO>()
                .eq(AiKnowledgeChunkDO::getDocumentId, documentId));
    }

    private void markChunkIndexed(AiKnowledgeChunkDO chunk, AiModelDO model, float[] vector) {
        String modelName = StrUtil.brief(StrUtil.nullToEmpty(model.getModelName()), 100);
        chunk.setEmbedStatus(EMBED_INDEXED);
        chunk.setEmbeddingModel(modelName);
        chunk.setEmbeddingDim(vector.length);
        chunk.setEmbeddingVector(toVectorJson(vector));
        chunk.setErrorMsg("");
        chunkMapper.update(null, new LambdaUpdateWrapper<AiKnowledgeChunkDO>()
                .eq(AiKnowledgeChunkDO::getId, chunk.getId())
                .set(AiKnowledgeChunkDO::getEmbedStatus, EMBED_INDEXED)
                .set(AiKnowledgeChunkDO::getEmbeddingModel, modelName)
                .set(AiKnowledgeChunkDO::getEmbeddingDim, vector.length)
                .set(AiKnowledgeChunkDO::getEmbeddingVector, chunk.getEmbeddingVector())
                .set(AiKnowledgeChunkDO::getErrorMsg, ""));
    }

    private void markChunkFailed(AiKnowledgeChunkDO chunk, String error) {
        String message = StrUtil.brief(StrUtil.nullToEmpty(error), ERROR_MSG_MAX);
        chunk.setEmbedStatus(EMBED_FAILED);
        chunk.setErrorMsg(message);
        chunkMapper.update(null, new LambdaUpdateWrapper<AiKnowledgeChunkDO>()
                .eq(AiKnowledgeChunkDO::getId, chunk.getId())
                .set(AiKnowledgeChunkDO::getEmbedStatus, EMBED_FAILED)
                .set(AiKnowledgeChunkDO::getErrorMsg, message));
    }

    private void updateDocument(Long documentId, String parseStatus, String content, Integer charCount) {
        updateDocument(documentId, parseStatus, content, charCount, null);
    }

    private void updateDocument(Long documentId, String parseStatus, String content,
                                Integer charCount, String errorMsg) {
        LambdaUpdateWrapper<AiKnowledgeDocumentDO> wrapper = new LambdaUpdateWrapper<AiKnowledgeDocumentDO>()
                .eq(AiKnowledgeDocumentDO::getId, documentId)
                .set(AiKnowledgeDocumentDO::getParseStatus, parseStatus)
                .set(errorMsg != null, AiKnowledgeDocumentDO::getErrorMsg, StrUtil.brief(errorMsg, ERROR_MSG_MAX))
                .set(errorMsg == null, AiKnowledgeDocumentDO::getErrorMsg, "");
        if (content != null) {
            wrapper.set(AiKnowledgeDocumentDO::getContent, content)
                    .set(AiKnowledgeDocumentDO::getCharCount, charCount == null ? content.length() : charCount);
        }
        documentMapper.update(null, wrapper);
    }

    private void updateDocumentIndexStatus(Long documentId, String indexStatus, String errorMsg) {
        documentMapper.update(null, new LambdaUpdateWrapper<AiKnowledgeDocumentDO>()
                .eq(AiKnowledgeDocumentDO::getId, documentId)
                .set(AiKnowledgeDocumentDO::getIndexStatus, indexStatus)
                .set(AiKnowledgeDocumentDO::getErrorMsg,
                        errorMsg == null ? "" : StrUtil.brief(errorMsg, ERROR_MSG_MAX)));
    }

    /** 向量序列化为 JSON 数组，用于 MySQL 副本与重启恢复 */
    private String toVectorJson(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 12);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * 从数据库副本解析向量（供启动恢复使用）。
     */
    public static float[] parseVector(String json) {
        if (StrUtil.isBlank(json)) {
            return new float[0];
        }
        List<Float> values = JSONUtil.parseArray(json).toList(Float.class);
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i) == null ? 0f : values.get(i);
        }
        return vector;
    }
}
