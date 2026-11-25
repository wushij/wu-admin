package com.admin.server.modules.ai.convert;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBaseRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBaseSaveReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeDocumentRespVO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeBaseDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeChunkDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDocumentDO;

/**
 * AI 知识库（RAG）DO ↔ VO 手写转换器
 */
public final class AiKnowledgeRagConvert {

    private AiKnowledgeRagConvert() {
    }

    public static AiKnowledgeBaseRespVO baseToResp(AiKnowledgeBaseDO base, String embeddingModelName,
                                                   Integer indexedChunkCount) {
        if (base == null) {
            return null;
        }
        AiKnowledgeBaseRespVO vo = new AiKnowledgeBaseRespVO();
        vo.setId(base.getId());
        vo.setName(base.getName());
        vo.setDescription(base.getDescription());
        vo.setEmbeddingModelId(base.getEmbeddingModelId());
        vo.setEmbeddingModelName(embeddingModelName);
        vo.setChunkSize(base.getChunkSize());
        vo.setChunkOverlap(base.getChunkOverlap());
        vo.setDocumentCount(base.getDocumentCount());
        vo.setChunkCount(base.getChunkCount());
        vo.setIndexedChunkCount(indexedChunkCount);
        vo.setIndexStatus(base.getIndexStatus());
        vo.setStatus(base.getStatus());
        vo.setSort(base.getSort());
        vo.setCreateTime(base.getCreateTime());
        vo.setUpdateTime(base.getUpdateTime());
        return vo;
    }

    public static AiKnowledgeBaseDO baseSaveToDO(AiKnowledgeBaseSaveReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        AiKnowledgeBaseDO base = new AiKnowledgeBaseDO();
        base.setId(reqVO.getId());
        base.setName(reqVO.getName());
        base.setDescription(reqVO.getDescription());
        base.setEmbeddingModelId(reqVO.getEmbeddingModelId());
        base.setChunkSize(reqVO.getChunkSize());
        base.setChunkOverlap(reqVO.getChunkOverlap());
        base.setStatus(reqVO.getStatus());
        base.setSort(reqVO.getSort());
        return base;
    }

    public static AiKnowledgeDocumentRespVO documentToResp(AiKnowledgeDocumentDO document) {
        if (document == null) {
            return null;
        }
        AiKnowledgeDocumentRespVO vo = new AiKnowledgeDocumentRespVO();
        vo.setId(document.getId());
        vo.setKnowledgeBaseId(document.getKnowledgeBaseId());
        vo.setFileId(document.getFileId());
        vo.setFileName(document.getFileName());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setCharCount(document.getCharCount());
        vo.setChunkCount(document.getChunkCount());
        vo.setParseStatus(document.getParseStatus());
        vo.setIndexStatus(document.getIndexStatus());
        vo.setErrorMsg(document.getErrorMsg());
        vo.setStatus(document.getStatus());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());
        return vo;
    }

    public static AiKnowledgeChunkRespVO chunkToResp(AiKnowledgeChunkDO chunk, String documentName) {
        if (chunk == null) {
            return null;
        }
        AiKnowledgeChunkRespVO vo = new AiKnowledgeChunkRespVO();
        vo.setId(chunk.getId());
        vo.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
        vo.setDocumentId(chunk.getDocumentId());
        vo.setDocumentName(StrUtil.nullToEmpty(documentName));
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setHeading(chunk.getHeading());
        vo.setContent(chunk.getContent());
        vo.setCharCount(chunk.getCharCount());
        vo.setTokenEstimate(chunk.getTokenEstimate());
        vo.setEmbedStatus(chunk.getEmbedStatus());
        vo.setEmbeddingModel(chunk.getEmbeddingModel());
        vo.setEmbeddingDim(chunk.getEmbeddingDim());
        vo.setErrorMsg(chunk.getErrorMsg());
        vo.setCreateTime(chunk.getCreateTime());
        return vo;
    }
}
