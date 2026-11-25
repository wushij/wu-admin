package com.admin.server.modules.ai.service;

import com.admin.server.modules.ai.api.vo.AiChatHistoryItemVO;
import com.admin.server.modules.ai.api.vo.AiConversationVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;

import java.util.List;

/**
 * AI 助手对话会话持久化服务（仅服务于 AI wu 助手历史会话恢复与管理）
 */
public interface AiChatLogService {

    /** 异步落库对话记录 */
    void saveLogAsync(AiChatLogDO chatLog);

    /** 同步落库对话记录，返回记录ID（供流式结束回传前端按轮删除） */
    Long saveLogSync(AiChatLogDO chatLog);

    /** 当前用户的历史会话列表（最近优先，仅本人） */
    List<AiConversationVO> listConversations(Long userId);

    /** 指定会话的问答序列（时间正序，仅本人，用于恢复续聊） */
    List<AiChatHistoryItemVO> listHistory(Long userId, String conversationId);

    /** 删除指定历史会话（仅本人） */
    void deleteConversation(Long userId, String conversationId);

    /** 清空当前用户的所有历史会话（仅本人） */
    void cleanUserConversations(Long userId);

    /** 删除本人的单轮问答记录（按消息/轮次ID），返回是否删除成功 */
    boolean deleteMessageRound(Long userId, Long logId);

    /** 删除本人指定会话的最后一条记录（用于中断/失败轮次尚未回传记录ID的场景） */
    boolean deleteLastRound(Long userId, String conversationId);
}
