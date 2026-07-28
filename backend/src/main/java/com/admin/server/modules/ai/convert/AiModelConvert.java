package com.admin.server.modules.ai.convert;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.ai.api.vo.AiChatLogRespVO;
import com.admin.server.modules.ai.api.vo.AiModelRespVO;
import com.admin.server.modules.ai.api.vo.AiModelSaveReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 模块 DO ↔ VO 手写转换器（对齐 system 模块 Convert 规范）
 */
public final class AiModelConvert {

    private AiModelConvert() {
    }

    /** DO → RespVO，apiKey 转掩码，绝不泄露密文 */
    public static AiModelRespVO convertModel(AiModelDO model, String plainApiKey) {
        if (model == null) {
            return null;
        }
        AiModelRespVO vo = new AiModelRespVO();
        vo.setId(model.getId());
        vo.setName(model.getName());
        vo.setProvider(model.getProvider());
        vo.setModelName(model.getModelName());
        vo.setBaseUrl(model.getBaseUrl());
        vo.setHasApiKey(StrUtil.isNotBlank(model.getApiKey()));
        vo.setApiKeyMasked(maskApiKey(plainApiKey));
        vo.setTemperature(model.getTemperature());
        vo.setMaxTokens(model.getMaxTokens());
        vo.setSystemPrompt(model.getSystemPrompt());
        vo.setIsDefault(model.getIsDefault());
        vo.setStatus(model.getStatus());
        vo.setRemark(model.getRemark());
        vo.setCreateTime(model.getCreateTime());
        vo.setUpdateTime(model.getUpdateTime());
        return vo;
    }

    /** SaveReqVO → DO（apiKey 由 Service 层加密后单独设置） */
    public static AiModelDO convertSave(AiModelSaveReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        AiModelDO model = new AiModelDO();
        model.setId(reqVO.getId());
        model.setName(reqVO.getName());
        model.setProvider(reqVO.getProvider());
        model.setModelName(reqVO.getModelName());
        model.setBaseUrl(reqVO.getBaseUrl());
        model.setTemperature(reqVO.getTemperature());
        model.setMaxTokens(reqVO.getMaxTokens());
        model.setSystemPrompt(reqVO.getSystemPrompt());
        model.setIsDefault(reqVO.getIsDefault());
        model.setStatus(reqVO.getStatus());
        model.setRemark(reqVO.getRemark());
        return model;
    }

    public static AiChatLogRespVO convertLog(AiChatLogDO log) {
        if (log == null) {
            return null;
        }
        AiChatLogRespVO vo = new AiChatLogRespVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setConversationId(log.getConversationId());
        vo.setModelId(log.getModelId());
        vo.setProvider(log.getProvider());
        vo.setModelName(log.getModelName());
        vo.setQuestion(log.getQuestion());
        vo.setAnswer(log.getAnswer());
        vo.setPromptTokens(log.getPromptTokens());
        vo.setCompletionTokens(log.getCompletionTokens());
        vo.setTotalTokens(log.getTotalTokens());
        vo.setDurationMs(log.getDurationMs());
        vo.setChatStatus(log.getChatStatus());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setSource(log.getSource());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    public static List<AiChatLogRespVO> convertLogList(List<AiChatLogDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<AiChatLogRespVO> result = new ArrayList<>(list.size());
        for (AiChatLogDO log : list) {
            result.add(convertLog(log));
        }
        return result;
    }

    /** API Key 掩码：保留前 4 后 4 位，中间以 *** 代替 */
    public static String maskApiKey(String plainKey) {
        if (StrUtil.isBlank(plainKey)) {
            return "";
        }
        if (plainKey.length() <= 8) {
            return plainKey.charAt(0) + "***";
        }
        return plainKey.substring(0, 4) + "***" + plainKey.substring(plainKey.length() - 4);
    }
}
