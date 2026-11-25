package com.admin.server.modules.ai.dal.mysql;

import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDocumentDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 知识库文档 Mapper
 */
@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocumentDO> {
}
