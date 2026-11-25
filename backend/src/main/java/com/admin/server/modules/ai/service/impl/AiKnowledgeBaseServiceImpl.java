package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBasePageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBaseRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBaseSaveReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkStatsVO;
import com.admin.server.modules.ai.api.vo.AiModelRespVO;
import com.admin.server.modules.ai.convert.AiKnowledgeRagConvert;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeBaseDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeChunkDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDocumentDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeBaseMapper;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeChunkMapper;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeDocumentMapper;
import com.admin.server.modules.ai.service.AiKnowledgeBaseService;
import com.admin.server.modules.ai.service.AiModelService;
import com.admin.server.modules.ai.service.rag.VectorStore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 知识库管理实现
 */
@Service
public class AiKnowledgeBaseServiceImpl extends ServiceImpl<AiKnowledgeBaseMapper, AiKnowledgeBaseDO>
        implements AiKnowledgeBaseService {

    private static final int MIN_CHUNK_SIZE = 256;
    private static final int MAX_CHUNK_SIZE = 4000;
    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_CHUNK_OVERLAP = 128;

    private static final String INDEX_IDLE = "IDLE";
    private static final String INDEX_INDEXING = "INDEXING";
    private static final String INDEX_INDEXED = "INDEXED";
    private static final String INDEX_FAILED = "FAILED";

    @Resource
    private AiKnowledgeDocumentMapper documentMapper;

    @Resource
    private AiKnowledgeChunkMapper chunkMapper;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private VectorStore vectorStore;

    @Override
    public PageResult<AiKnowledgeBaseRespVO> page(AiKnowledgeBasePageReqVO reqVO) {
        Page<AiKnowledgeBaseDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        LambdaQueryWrapper<AiKnowledgeBaseDO> wrapper = new LambdaQueryWrapper<AiKnowledgeBaseDO>()
                .like(StrUtil.isNotBlank(reqVO.getName()), AiKnowledgeBaseDO::getName, reqVO.getName())
                .eq(reqVO.getStatus() != null, AiKnowledgeBaseDO::getStatus, reqVO.getStatus())
                .eq(StrUtil.isNotBlank(reqVO.getIndexStatus()), AiKnowledgeBaseDO::getIndexStatus, reqVO.getIndexStatus())
                .orderByDesc(AiKnowledgeBaseDO::getSort)
                .orderByDesc(AiKnowledgeBaseDO::getUpdateTime);
        Page<AiKnowledgeBaseDO> result = page(page, wrapper);
        List<AiKnowledgeBaseRespVO> voList = new ArrayList<>(result.getRecords().size());
        for (AiKnowledgeBaseDO base : result.getRecords()) {
            voList.add(AiKnowledgeRagConvert.baseToResp(base,
                    resolveEmbeddingModelName(base.getEmbeddingModelId()), null));
        }
        return PageResult.of(voList, result.getTotal());
    }

    @Override
    public AiKnowledgeBaseRespVO detail(Long id) {
        AiKnowledgeBaseDO base = getBaseOrThrow(id);
        AiKnowledgeChunkStatsVO stats = chunkMapper.selectStats(id);
        Integer indexed = stats == null || stats.getIndexed() == null ? 0 : stats.getIndexed().intValue();
        return AiKnowledgeRagConvert.baseToResp(base,
                resolveEmbeddingModelName(base.getEmbeddingModelId()), indexed);
    }

    @Override
    public List<AiKnowledgeBaseRespVO> listEnabled() {
        List<AiKnowledgeBaseDO> list = list(new LambdaQueryWrapper<AiKnowledgeBaseDO>()
                .eq(AiKnowledgeBaseDO::getStatus, 1)
                .orderByDesc(AiKnowledgeBaseDO::getSort)
                .orderByDesc(AiKnowledgeBaseDO::getUpdateTime));
        List<AiKnowledgeBaseRespVO> voList = new ArrayList<>(list.size());
        for (AiKnowledgeBaseDO base : list) {
            voList.add(AiKnowledgeRagConvert.baseToResp(base,
                    resolveEmbeddingModelName(base.getEmbeddingModelId()), null));
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AiKnowledgeBaseSaveReqVO reqVO) {
        validateSaveReq(reqVO, null);
        AiKnowledgeBaseDO base = AiKnowledgeRagConvert.baseSaveToDO(reqVO);
        base.setName(StrUtil.trim(reqVO.getName()));
        base.setDescription(StrUtil.nullToEmpty(reqVO.getDescription()));
        base.setChunkSize(normalizeChunkSize(reqVO.getChunkSize()));
        base.setChunkOverlap(normalizeOverlap(reqVO.getChunkOverlap(), base.getChunkSize()));
        base.setEmbeddingModelId(reqVO.getEmbeddingModelId() == null ? 0L : reqVO.getEmbeddingModelId());
        base.setDocumentCount(0);
        base.setChunkCount(0);
        base.setIndexStatus(INDEX_IDLE);
        base.setStatus(reqVO.getStatus() == null ? 1 : reqVO.getStatus());
        base.setSort(reqVO.getSort() == null ? 0 : reqVO.getSort());
        save(base);
        return base.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AiKnowledgeBaseSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw new BusinessException("知识库 ID 不能为空");
        }
        AiKnowledgeBaseDO old = getBaseOrThrow(reqVO.getId());
        validateSaveReq(reqVO, reqVO.getId());
        AiKnowledgeBaseDO update = new AiKnowledgeBaseDO();
        update.setId(reqVO.getId());
        update.setName(StrUtil.trim(reqVO.getName()));
        update.setDescription(StrUtil.nullToEmpty(reqVO.getDescription()));
        update.setChunkSize(normalizeChunkSize(reqVO.getChunkSize()));
        update.setChunkOverlap(normalizeOverlap(reqVO.getChunkOverlap(), update.getChunkSize()));
        update.setEmbeddingModelId(reqVO.getEmbeddingModelId() == null ? 0L : reqVO.getEmbeddingModelId());
        update.setStatus(reqVO.getStatus());
        update.setSort(reqVO.getSort());
        updateById(update);
        // 切片参数或向量模型变化后，已有向量不再可信，提示重建索引
        boolean needsReindex = !java.util.Objects.equals(old.getChunkSize(), update.getChunkSize())
                || !java.util.Objects.equals(old.getChunkOverlap(), update.getChunkOverlap())
                || !java.util.Objects.equals(old.getEmbeddingModelId(), update.getEmbeddingModelId());
        if (needsReindex) {
            update(new LambdaUpdateWrapper<AiKnowledgeBaseDO>()
                    .eq(AiKnowledgeBaseDO::getId, reqVO.getId())
                    .set(AiKnowledgeBaseDO::getIndexStatus, INDEX_IDLE));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getBaseOrThrow(id);
        removeById(id);
        documentMapper.delete(new LambdaQueryWrapper<AiKnowledgeDocumentDO>()
                .eq(AiKnowledgeDocumentDO::getKnowledgeBaseId, id));
        chunkMapper.delete(new LambdaQueryWrapper<AiKnowledgeChunkDO>()
                .eq(AiKnowledgeChunkDO::getKnowledgeBaseId, id));
        vectorStore.clearKnowledgeBase(id);
    }

    @Override
    public void refreshStats(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return;
        }
        Long documentCount = documentMapper.selectCount(new LambdaQueryWrapper<AiKnowledgeDocumentDO>()
                .eq(AiKnowledgeDocumentDO::getKnowledgeBaseId, knowledgeBaseId));
        AiKnowledgeChunkStatsVO stats = chunkMapper.selectStats(knowledgeBaseId);
        long total = stats == null || stats.getTotal() == null ? 0 : stats.getTotal();
        long indexed = stats == null || stats.getIndexed() == null ? 0 : stats.getIndexed();
        long failed = stats == null || stats.getFailed() == null ? 0 : stats.getFailed();
        String indexStatus;
        if (total == 0) {
            indexStatus = INDEX_IDLE;
        } else if (indexed >= total) {
            indexStatus = INDEX_INDEXED;
        } else if (indexed > 0) {
            indexStatus = failed > 0 ? INDEX_FAILED : INDEX_INDEXING;
        } else {
            indexStatus = failed > 0 ? INDEX_FAILED : INDEX_IDLE;
        }
        update(new LambdaUpdateWrapper<AiKnowledgeBaseDO>()
                .eq(AiKnowledgeBaseDO::getId, knowledgeBaseId)
                .set(AiKnowledgeBaseDO::getDocumentCount, documentCount == null ? 0 : documentCount.intValue())
                .set(AiKnowledgeBaseDO::getChunkCount, (int) total)
                .set(AiKnowledgeBaseDO::getIndexStatus, indexStatus));
    }

    @Override
    public AiKnowledgeChunkStatsVO chunkStats(Long knowledgeBaseId) {
        getBaseOrThrow(knowledgeBaseId);
        AiKnowledgeChunkStatsVO stats = chunkMapper.selectStats(knowledgeBaseId);
        if (stats == null) {
            stats = new AiKnowledgeChunkStatsVO();
            stats.setTotal(0L);
            stats.setIndexed(0L);
            stats.setPending(0L);
            stats.setFailed(0L);
            stats.setAvgTokens(0L);
            stats.setTotalTokens(0L);
        }
        return stats;
    }

    @Override
    public AiKnowledgeBaseDO getBaseOrThrow(Long id) {
        AiKnowledgeBaseDO base = id == null ? null : getById(id);
        if (base == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        return base;
    }

    private void validateSaveReq(AiKnowledgeBaseSaveReqVO reqVO, Long excludeId) {
        if (StrUtil.isBlank(reqVO.getName())) {
            throw new BusinessException("知识库名称不能为空");
        }
        if (reqVO.getName().trim().length() > 128) {
            throw new BusinessException("知识库名称不能超过 128 个字符");
        }
        Long sameName = count(new LambdaQueryWrapper<AiKnowledgeBaseDO>()
                .eq(AiKnowledgeBaseDO::getName, reqVO.getName().trim())
                .ne(excludeId != null, AiKnowledgeBaseDO::getId, excludeId));
        if (sameName != null && sameName > 0) {
            throw new BusinessException("知识库名称已存在，请更换名称");
        }
        if (reqVO.getChunkSize() != null
                && (reqVO.getChunkSize() < MIN_CHUNK_SIZE || reqVO.getChunkSize() > MAX_CHUNK_SIZE)) {
            throw new BusinessException("切片大小取值范围为 " + MIN_CHUNK_SIZE + " ~ " + MAX_CHUNK_SIZE);
        }
        if (reqVO.getEmbeddingModelId() != null && reqVO.getEmbeddingModelId() > 0) {
            // 校验所选的必须是启用中的向量模型
            aiModelService.getAvailableEmbeddingModel(reqVO.getEmbeddingModelId());
        }
    }

    private int normalizeChunkSize(Integer chunkSize) {
        int size = chunkSize == null ? DEFAULT_CHUNK_SIZE : chunkSize;
        return Math.max(MIN_CHUNK_SIZE, Math.min(size, MAX_CHUNK_SIZE));
    }

    private int normalizeOverlap(Integer overlap, Integer chunkSize) {
        int base = chunkSize == null ? DEFAULT_CHUNK_SIZE : chunkSize;
        int value = overlap == null ? DEFAULT_CHUNK_OVERLAP : overlap;
        value = Math.max(0, value);
        return Math.min(value, base / 2);
    }

    /** 知识库独立配置了向量模型时取其名称，否则展示默认向量模型名称 */
    private String resolveEmbeddingModelName(Long embeddingModelId) {
        List<AiModelRespVO> models = aiModelService.listEnabledEmbedding();
        if (models == null || models.isEmpty()) {
            return "";
        }
        if (embeddingModelId != null && embeddingModelId > 0) {
            for (AiModelRespVO model : models) {
                if (embeddingModelId.equals(model.getId())) {
                    return model.getName();
                }
            }
        }
        // listEnabledEmbedding 已按「默认优先」排序，首个即默认向量模型
        return models.get(0).getName();
    }
}
