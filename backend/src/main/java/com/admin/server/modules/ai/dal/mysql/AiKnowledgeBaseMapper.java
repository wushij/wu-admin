package com.admin.server.modules.ai.dal.mysql;

import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeBaseDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 知识库 Mapper
 */
@Mapper
public interface AiKnowledgeBaseMapper extends BaseMapper<AiKnowledgeBaseDO> {
}
