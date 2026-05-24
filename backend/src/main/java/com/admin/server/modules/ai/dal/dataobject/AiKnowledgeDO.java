package com.admin.server.modules.ai.dal.dataobject;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库 DO，映射 sys_ai_knowledge（L2 RAG-lite 知识检索）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_knowledge")
public class AiKnowledgeDO extends BaseEntity {
    /** 知识标题 */
    private String title;
    /** 检索关键词（逗号分隔） */
    private String keywords;
    /** 知识正文（Markdown） */
    private String content;
    /** 分类: faq/manual/module/other */
    private String category;
    /** 排序权重（大者优先） */
    private Integer sort;
    /** 状态: 1启用 0停用 */
    private Integer status;
}
