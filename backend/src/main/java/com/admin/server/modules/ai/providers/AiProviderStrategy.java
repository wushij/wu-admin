package com.admin.server.modules.ai.providers;

import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;

import java.util.List;

/**
 * 大模型供应商统一策略接口（策略模式）
 */
public interface AiProviderStrategy {

    /** 是否支持该供应商标识 */
    boolean supports(String provider);

    /**
     * 流式对话
     *
     * @param model       模型配置
     * @param plainApiKey 解密后的明文 API Key
     * @param messages    多轮消息（已脱敏，不含 system 角色，由 Provider 按需拼接 systemPrompt）
     * @param listener    增量回调
     */
    void streamChat(AiModelDO model, String plainApiKey,
                    List<AiChatStreamReqVO.ChatMessage> messages, AiStreamListener listener) throws Exception;

    /**
     * 连通性测试
     *
     * @return 往返延迟（毫秒）
     */
    long testConnection(AiModelDO model, String plainApiKey) throws Exception;
}
