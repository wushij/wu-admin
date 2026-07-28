package com.admin.server.modules.ai.dal.dataobject;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 对话日志 DO，映射 sys_ai_chat_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_chat_log")
public class AiChatLogDO extends BaseEntity {
    /** 提问用户ID */
    private Long userId;
    /** 提问用户名 */
    private String username;
    /** 会话ID（前端生成，串联多轮对话） */
    private String conversationId;
    /** 模型配置ID */
    private Long modelId;
    /** 供应商 */
    private String provider;
    /** 模型名称 */
    private String modelName;
    /** 用户提问（脱敏后） */
    private String question;
    /** AI 回答 */
    private String answer;
    /** 提问 Token 消耗 */
    private Integer promptTokens;
    /** 回答 Token 消耗 */
    private Integer completionTokens;
    /** 总 Token 消耗 */
    private Integer totalTokens;
    /** 耗时（毫秒） */
    private Long durationMs;
    /** 结果 1:成功 0:失败 2:用户中断 */
    private Integer chatStatus;
    /** 失败原因 */
    private String errorMsg;
    /** 来源终端 pc/mobile */
    private String source;
}
