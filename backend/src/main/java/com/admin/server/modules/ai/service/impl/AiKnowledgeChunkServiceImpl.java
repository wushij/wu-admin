package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkPageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkStatsVO;
import com.admin.server.modules.ai.convert.AiKnowledgeRagConvert;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeChunkDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDocumentDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeChunkMapper;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeDocumentMapper;
import com.admin.server.modules.ai.service.AiKnowledgeBaseService;
import com.admin.server.modules.ai.service.AiKnowledgeChunkService;
import com.admin.server.modules.ai.service.rag.KnowledgeIndexService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 知识库切片管理实现
 */
@Service
public class AiKnowledgeChunkServiceImpl extends ServiceImpl<AiKnowledgeChunkMapper, AiKnowledgeChunkDO>
        implements AiKnowledgeChunkService {

    @Resource
    private AiKnowledgeDocumentMapper documentMapper;

    @Resource
    private AiKnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeIndexService knowledgeIndexService;

    @Override
    public PageResult<AiKnowledgeChunkRespVO> page(AiKnowledgeChunkPageReqVO reqVO) {
        Page<AiKnowledgeChunkDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        LambdaQueryWrapper<AiKnowledgeChunkDO> wrapper = new LambdaQueryWrapper<AiKnowledgeChunkDO>()
                .eq(AiKnowledgeChunkDO::getKnowledgeBaseId, reqVO.getKnowledgeBaseId())
                .eq(reqVO.getDocumentId() != null, AiKnowledgeChunkDO::getDocumentId, reqVO.getDocumentId())
                .eq(StrUtil.isNotBlank(reqVO.getEmbedStatus()),
                        AiKnowledgeChunkDO::getEmbedStatus, reqVO.getEmbedStatus())
                .and(StrUtil.isNotBlank(reqVO.getKeyword()), w -> w
                        .like(AiKnowledgeChunkDO::getContent, reqVO.getKeyword())
                        .or().like(AiKnowledgeChunkDO::getHeading, reqVO.getKeyword()))
                .orderByAsc(AiKnowledgeChunkDO::getDocumentId)
                .orderByAsc(AiKnowledgeChunkDO::getChunkIndex);
        Page<AiKnowledgeChunkDO> result = page(page, wrapper);
        Map<Long, String> documentNames = loadDocumentNames(result.getRecords());
        List<AiKnowledgeChunkRespVO> voList = new ArrayList<>(result.getRecords().size());
        for (AiKnowledgeChunkDO chunk : result.getRecords()) {
            voList.add(AiKnowledgeRagConvert.chunkToResp(chunk, documentNames.get(chunk.getDocumentId())));
        }
        return PageResult.of(voList, result.getTotal());
    }

    @Override
    public AiKnowledgeChunkRespVO detail(Long id) {
        AiKnowledgeChunkDO chunk = getChunkOrThrow(id);
        AiKnowledgeDocumentDO document = documentMapper.selectById(chunk.getDocumentId());
        return AiKnowledgeRagConvert.chunkToResp(chunk,
                document == null ? "" : document.getFileName());
    }

    @Override
    public AiKnowledgeChunkStatsVO stats(Long knowledgeBaseId) {
        return knowledgeBaseService.chunkStats(knowledgeBaseId);
    }

    @Override
    public void reindex(Long chunkId) {
        AiKnowledgeChunkDO chunk = getChunkOrThrow(chunkId);
        try {
            knowledgeIndexService.reindexChunk(chunkId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("重建向量失败: " + e.getMessage());
        }
        knowledgeBaseService.refreshStats(chunk.getKnowledgeBaseId());
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

    private AiKnowledgeChunkDO getChunkOrThrow(Long id) {
        AiKnowledgeChunkDO chunk = id == null ? null : getById(id);
        if (chunk == null) {
            throw new BusinessException(404, "切片不存在");
        }
        return chunk;
    }
}
