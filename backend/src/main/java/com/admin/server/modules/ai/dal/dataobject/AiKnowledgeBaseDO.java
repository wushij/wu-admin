package com.admin.server.modules.ai.dal.dataobject;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库 DO，映射 sys_ai_knowledge_base
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_knowledge_base")
public class AiKnowledgeBaseDO extends BaseEntity {
    /** 知识库名称 */
    private String name;
    /** 知识库描述 */
    private String description;
    /** 向量模型 ID（0=跟随默认向量模型） */
    private Long embeddingModelId;
    /** 切片目标字符数 */
    private Integer chunkSize;
    /** 切片重叠字符数 */
    private Integer chunkOverlap;
    /** 文档数（冗余统计） */
    private Integer documentCount;
    /** 切片数（冗余统计） */
    private Integer chunkCount;
    /** 索引状态：IDLE/INDEXING/INDEXED/FAILED */
    private String indexStatus;
    /** 状态：1启用 0停用 */
    private Integer status;
    /** 排序权重（大者优先） */
    private Integer sort;
}
