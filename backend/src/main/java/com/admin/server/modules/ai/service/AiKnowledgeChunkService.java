package com.admin.server.modules.ai.service;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkPageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkStatsVO;

/**
 * AI 知识库切片管理（切片层）
 */
public interface AiKnowledgeChunkService {

    PageResult<AiKnowledgeChunkRespVO> page(AiKnowledgeChunkPageReqVO reqVO);

    AiKnowledgeChunkRespVO detail(Long id);

    AiKnowledgeChunkStatsVO stats(Long knowledgeBaseId);

    /** 重建单个切片的向量（同步执行，失败抛出具体原因） */
    void reindex(Long chunkId);
}
