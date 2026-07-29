package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 历史会话单轮问答（恢复续聊用）
 */
@Data
public class AiChatHistoryItemVO {
    /** 用户提问（已脱敏） */
    private String question;
    /** AI 回答（失败轮为空） */
    private String answer;
    /** 对话时间 */
    private LocalDateTime createTime;
}
