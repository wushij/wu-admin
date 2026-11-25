package com.admin.server.modules.ai.dal.dataobject;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库文档 DO，映射 sys_ai_knowledge_document
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_knowledge_document")
public class AiKnowledgeDocumentDO extends BaseEntity {
    /** 所属知识库 ID */
    private Long knowledgeBaseId;
    /** 迁移来源：旧 sys_ai_knowledge.id，0=非迁移 */
    private Long legacyKnowledgeId;
    /** 原始文件 ID（sys_file），迁移数据为 0 */
    private Long fileId;
    /** 文件名 */
    private String fileName;
    /** 文件类型：md/txt/pdf/docx */
    private String fileType;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 正文哈希（去重） */
    private String contentHash;
    /** 解析后的正文 */
    private String content;
    /** 正文字符数 */
    private Integer charCount;
    /** 切片数 */
    private Integer chunkCount;
    /** 解析状态：PENDING/PARSING/SUCCESS/FAILED */
    private String parseStatus;
    /** 向量状态：PENDING/INDEXING/INDEXED/PARTIAL/FAILED */
    private String indexStatus;
    /** 最近一次处理失败原因 */
    private String errorMsg;
    /** 状态：1启用 0停用 */
    private Integer status;
}
