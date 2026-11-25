package com.admin.server.modules.ai.api.vo;

import com.admin.server.modules.ai.service.search.vo.WebSearchCitationVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 历史会话单轮问答（恢复续聊用）
 */
@Data
public class AiChatHistoryItemVO {
    /** 对话记录ID（用于按轮删除） */
    private Long id;
    /** 用户提问（已脱敏） */
    private String question;
    /** AI 回答（失败轮为空） */
    private String answer;
    /** 深度思考过程正文 */
    private String reasoning;
    /** 深度思考耗时（毫秒） */
    private Long thinkingDurationMs;
    /** 工具/联网执行轨迹摘要文案（用于历史回放） */
    private List<String> toolMessages;
    /** 本轮用户附件元数据 */
    private List<AiChatAttachmentVO> attachments;
    /** 本轮附件正文快照（续聊回放上下文） */
    private String attachmentContext;
    /** 本轮联网检索参考来源列表 */
    private List<WebSearchCitationVO> citations;
    /** 对话时间 */
    private LocalDateTime createTime;
}
