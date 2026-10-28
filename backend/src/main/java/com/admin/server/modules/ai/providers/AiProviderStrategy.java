package com.admin.server.modules.ai.providers;

import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;

import java.util.List;

/**
 * 大模型接入协议统一策略接口（策略模式）
 * <p>按「协议」而不是「供应商」分发：DeepSeek / Kimi / GLM / 通义 / 豆包 / Ollama 等
 * 都走 OpenAiCompatibleProvider；Claude 及其兼容网关走 AnthropicProvider。</p>
 */
public interface AiProviderStrategy {

    /** 协议标识：openai / anthropic */
    String protocol();

    /**
     * 流式对话
     *
     * @param model       模型配置
     * @param plainApiKey 解密后的明文 API Key
     * @param messages    多轮消息（已脱敏，system 由服务层组装后置于首位）
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

    /**
     * 拉取该供应商可用的模型标识列表（GET /models）
     * <p>用于配置页的「获取模型」按钮，避免用户手抄模型名。</p>
     */
    List<String> listModels(AiModelDO model, String plainApiKey) throws Exception;
}
