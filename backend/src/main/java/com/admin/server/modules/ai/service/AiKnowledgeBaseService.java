package com.admin.server.modules.ai.service;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBasePageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBaseRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBaseSaveReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkStatsVO;

import java.util.List;

/**
 * AI 知识库管理（知识库层）
 */
public interface AiKnowledgeBaseService {

    PageResult<AiKnowledgeBaseRespVO> page(AiKnowledgeBasePageReqVO reqVO);

    AiKnowledgeBaseRespVO detail(Long id);

    /** 启用中的知识库列表（供检索测试与下拉选择） */
    List<AiKnowledgeBaseRespVO> listEnabled();

    Long create(AiKnowledgeBaseSaveReqVO reqVO);

    void update(AiKnowledgeBaseSaveReqVO reqVO);

    /** 逻辑删除知识库及其文档、切片，并清理向量索引 */
    void delete(Long id);

    /** 重算文档数、切片数与索引状态（文档增删、切片重建后调用） */
    void refreshStats(Long knowledgeBaseId);

    /** 切片统计（详情页指标卡） */
    AiKnowledgeChunkStatsVO chunkStats(Long knowledgeBaseId);

    /** 获取知识库并校验存在 */
    com.admin.server.modules.ai.dal.dataobject.AiKnowledgeBaseDO getBaseOrThrow(Long id);
}
