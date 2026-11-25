package com.admin.server.modules.ai.dal.dataobject;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库切片 DO，映射 sys_ai_knowledge_chunk
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_knowledge_chunk")
public class AiKnowledgeChunkDO extends BaseEntity {
    /** 所属知识库 ID */
    private Long knowledgeBaseId;
    /** 所属文档 ID */
    private Long documentId;
    /** 文档内切片序号（从 0 开始） */
    private Integer chunkIndex;
    /** 切片所属标题 */
    private String heading;
    /** 切片正文 */
    private String content;
    /** 字符数 */
    private Integer charCount;
    /** Token 估算值 */
    private Integer tokenEstimate;
    /** 向量状态：PENDING/INDEXED/FAILED */
    private String embedStatus;
    /** 向量化使用的模型名 */
    private String embeddingModel;
    /** 向量维度 */
    private Integer embeddingDim;
    /** 向量 JSON 数组（进程内索引恢复用） */
    private String embeddingVector;
    /** 向量化失败原因 */
    private String errorMsg;
}
