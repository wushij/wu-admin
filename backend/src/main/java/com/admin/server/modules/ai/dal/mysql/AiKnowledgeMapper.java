package com.admin.server.modules.ai.dal.mysql;

import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiKnowledgeMapper extends BaseMapper<AiKnowledgeDO> {

    /**
     * 全文检索（ngram），仅返回启用条目，按相关度 + 权重排序取前 limit 条
     * <p>BOOLEAN MODE 对中文短词更稳；MyBatis ${} 不适用故用 #{} 绑定，MATCH 表达式安全。</p>
     */
    @Select("""
            SELECT * FROM sys_ai_knowledge
            WHERE deleted = 0 AND status = 1
              AND MATCH(title, keywords, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY MATCH(title, keywords, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) DESC,
                     sort DESC, id ASC
            LIMIT #{limit}
            """)
    List<AiKnowledgeDO> searchFullText(@Param("query") String query, @Param("limit") int limit);
}
