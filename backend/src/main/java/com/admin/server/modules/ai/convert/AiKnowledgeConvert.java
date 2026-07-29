package com.admin.server.modules.ai.convert;

import com.admin.server.modules.ai.api.vo.AiKnowledgeRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeSaveReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 知识库 DO ↔ VO 手写转换器（对齐 system 模块 Convert 规范）
 */
public final class AiKnowledgeConvert {

    private AiKnowledgeConvert() {
    }

    public static AiKnowledgeRespVO convert(AiKnowledgeDO knowledge) {
        if (knowledge == null) {
            return null;
        }
        AiKnowledgeRespVO vo = new AiKnowledgeRespVO();
        vo.setId(knowledge.getId());
        vo.setTitle(knowledge.getTitle());
        vo.setKeywords(knowledge.getKeywords());
        vo.setContent(knowledge.getContent());
        vo.setCategory(knowledge.getCategory());
        vo.setSort(knowledge.getSort());
        vo.setStatus(knowledge.getStatus());
        vo.setCreator(knowledge.getCreator());
        vo.setUpdater(knowledge.getUpdater());
        vo.setCreateTime(knowledge.getCreateTime());
        vo.setUpdateTime(knowledge.getUpdateTime());
        return vo;
    }

    public static List<AiKnowledgeRespVO> convertList(List<AiKnowledgeDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<AiKnowledgeRespVO> result = new ArrayList<>(list.size());
        for (AiKnowledgeDO knowledge : list) {
            result.add(convert(knowledge));
        }
        return result;
    }

    public static AiKnowledgeDO convertSave(AiKnowledgeSaveReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        AiKnowledgeDO knowledge = new AiKnowledgeDO();
        knowledge.setId(reqVO.getId());
        knowledge.setTitle(reqVO.getTitle());
        knowledge.setKeywords(reqVO.getKeywords());
        knowledge.setContent(reqVO.getContent());
        knowledge.setCategory(reqVO.getCategory());
        knowledge.setSort(reqVO.getSort());
        knowledge.setStatus(reqVO.getStatus());
        return knowledge;
    }
}
