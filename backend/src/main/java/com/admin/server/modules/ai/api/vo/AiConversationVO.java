package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 历史会话摘要（悬浮窗历史对话列表）
 */
@Data
public class AiConversationVO {
    /** 会话ID */
    private String conversationId;
    /** 会话标题（该会话的首条提问，已脱敏） */
    private String title;
    /** 最后对话时间 */
    private LocalDateTime lastTime;
    /** 对话轮数 */
    private Integer messageCount;
}
