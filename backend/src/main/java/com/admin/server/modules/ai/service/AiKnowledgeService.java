package com.admin.server.modules.ai.service;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.ai.api.vo.AiKnowledgePageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeSaveReqVO;

/**
 * AI 知识库管理（L2 RAG-lite）
 */
public interface AiKnowledgeService {

    PageResult<AiKnowledgeRespVO> page(AiKnowledgePageReqVO reqVO);

    AiKnowledgeRespVO detail(Long id);

    void create(AiKnowledgeSaveReqVO reqVO);

    void update(AiKnowledgeSaveReqVO reqVO);

    void delete(Long id);
}
