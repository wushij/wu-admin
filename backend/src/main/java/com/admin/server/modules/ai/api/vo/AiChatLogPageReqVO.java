package com.admin.server.modules.ai.api.vo;

import com.admin.server.common.core.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 对话日志分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiChatLogPageReqVO extends PageParam {
    private String username;
    private String provider;
    private Integer chatStatus;
    private String conversationId;
}
