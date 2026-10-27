package com.admin.server.modules.ai.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 历史会话聚合数据对象（底层聚合查询专用）
 */
@Data
public class AiConversationDO {

    /** 会话ID */
    private String conversationId;

    /** 会话标题（首条提问） */
    private String title;

    /** 最后对话时间 */
    private LocalDateTime lastTime;

    /** 对话轮数 */
    private Integer messageCount;
}
