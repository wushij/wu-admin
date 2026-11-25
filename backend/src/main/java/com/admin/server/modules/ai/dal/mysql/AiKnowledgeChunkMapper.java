package com.admin.server.modules.ai.dal.mysql;

import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkStatsVO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeChunkDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 知识库切片 Mapper
 */
@Mapper
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunkDO> {

    /** 知识库切片统计（一次聚合，避免分页列表 N+1 计数） */
    @Select("""
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(embed_status = 'INDEXED'), 0) AS indexed,
                   COALESCE(SUM(embed_status = 'PENDING'), 0) AS pending,
                   COALESCE(SUM(embed_status = 'FAILED'), 0) AS failed,
                   COALESCE(ROUND(AVG(token_estimate)), 0) AS avgTokens,
                   COALESCE(SUM(token_estimate), 0) AS totalTokens
            FROM sys_ai_knowledge_chunk
            WHERE deleted = 0 AND knowledge_base_id = #{knowledgeBaseId}
            """)
    AiKnowledgeChunkStatsVO selectStats(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /** 启动恢复：按主键游标分页读取已向量化切片，避免一次性加载全表 */
    @Select("""
            SELECT id, knowledge_base_id, document_id, embedding_vector
            FROM sys_ai_knowledge_chunk
            WHERE deleted = 0 AND embed_status = 'INDEXED' AND embedding_vector IS NOT NULL
              AND id > #{cursor}
            ORDER BY id
            LIMIT #{limit}
            """)
    List<AiKnowledgeChunkDO> selectIndexedAfter(@Param("cursor") Long cursor, @Param("limit") int limit);
}
