package com.admin.server.modules.ai.service;

import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话流处理（脱敏 → 策略路由 → SSE 转发 → 异步落库）
 */
public interface AiChatService {

    /**
     * 流式对话
     *
     * @param reqVO    对话入参
     * @param userId   当前登录用户ID
     * @param username 当前登录用户名
     */
    SseEmitter stream(AiChatStreamReqVO reqVO, Long userId, String username);
}
