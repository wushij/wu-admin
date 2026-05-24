package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 流式对话入参
 */
@Data
public class AiChatStreamReqVO {
    /** 会话ID（前端生成 UUID，串联多轮对话） */
    private String conversationId;
    /** 指定模型配置ID，为空时使用系统默认模型 */
    private Long modelId;
    /** 来源终端 pc/mobile */
    private String source;
    /** 多轮消息上下文（含本轮用户提问，按时间正序） */
    private List<ChatMessage> messages;

    @Data
    public static class ChatMessage {
        /** 角色 user/assistant */
        private String role;
        /** 消息内容 */
        private String content;
    }
}
