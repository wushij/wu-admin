package com.admin.server.modules.ai.service;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.ai.api.vo.AiKnowledgeDocumentPageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeDocumentRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI 知识库文档管理（文档层）
 */
public interface AiKnowledgeDocumentService {

    PageResult<AiKnowledgeDocumentRespVO> page(AiKnowledgeDocumentPageReqVO reqVO);

    AiKnowledgeDocumentRespVO detail(Long id);

    /**
     * 批量上传文档：保存原文 → 建文档记录 → 事务提交后异步解析、切片、向量化。
     *
     * @return 每个文件的处理结果（fileName / success / message）
     */
    List<Map<String, Object>> upload(Long knowledgeBaseId, MultipartFile[] files);

    /** 重新解析：重置解析状态后异步执行解析 + 切片 + 向量化 */
    void reparse(Long id);

    /** 仅重新切片并向量化（正文已存在） */
    void reindex(Long id);

    /** 整个知识库重建索引 */
    void reindexAll(Long knowledgeBaseId);

    /** 逻辑删除文档、切片与向量 */
    void delete(Long id);

    void deleteBatch(List<Long> ids);

    /** 启用/停用文档：停用后立即从向量索引移除，避免继续被召回 */
    void updateStatus(Long id, Integer status);
}
