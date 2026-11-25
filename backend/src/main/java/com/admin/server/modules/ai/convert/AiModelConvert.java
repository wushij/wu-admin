package com.admin.server.modules.ai.convert;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.ai.api.vo.AiModelRespVO;
import com.admin.server.modules.ai.api.vo.AiModelSaveReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;

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
        vo.setProtocol(model.getProtocol());
        vo.setModelType(model.getModelType());
        vo.setModelName(model.getModelName());
        vo.setBaseUrl(model.getBaseUrl());
        vo.setPathTemplate(model.getPathTemplate());
        vo.setExtraHeaders(model.getExtraHeaders());
        vo.setHasApiKey(StrUtil.isNotBlank(model.getApiKey()));
        vo.setApiKeyMasked(maskApiKey(plainApiKey));
        vo.setTemperature(model.getTemperature());
        vo.setMaxTokens(model.getMaxTokens());
        vo.setDimension(model.getDimension());
        vo.setBatchSize(model.getBatchSize());
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
        model.setProtocol(reqVO.getProtocol());
        model.setModelType(reqVO.getModelType());
        model.setModelName(reqVO.getModelName());
        model.setBaseUrl(reqVO.getBaseUrl());
        model.setPathTemplate(reqVO.getPathTemplate());
        model.setExtraHeaders(reqVO.getExtraHeaders());
        model.setTemperature(reqVO.getTemperature());
        model.setMaxTokens(reqVO.getMaxTokens());
        model.setDimension(reqVO.getDimension());
        model.setBatchSize(reqVO.getBatchSize());
        model.setSystemPrompt(reqVO.getSystemPrompt());
        model.setIsDefault(reqVO.getIsDefault());
        model.setStatus(reqVO.getStatus());
        model.setRemark(reqVO.getRemark());
        return model;
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
