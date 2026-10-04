package com.admin.server.modules.ai.service;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.ai.api.vo.AiChatHistoryItemVO;
import com.admin.server.modules.ai.api.vo.AiChatLogPageReqVO;
import com.admin.server.modules.ai.api.vo.AiConversationVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;

import java.util.List;

/**
 * AI 对话日志审计
 */
public interface AiChatLogService {

    PageResult<AiChatLogDO> page(AiChatLogPageReqVO reqVO);

    void delete(Long id);

    void cleanLogs();

    /** 异步落库对话日志 */
    void saveLogAsync(AiChatLogDO chatLog);

    /** 当前用户的历史会话列表（最近优先，仅本人） */
    List<AiConversationVO> listConversations(Long userId);

    /** 指定会话的问答序列（时间正序，仅本人，用于恢复续聊） */
    List<AiChatHistoryItemVO> listHistory(Long userId, String conversationId);

    /** 删除指定历史会话（仅本人） */
    void deleteConversation(Long userId, String conversationId);

    /** 清空当前用户的所有历史会话（仅本人） */
    void cleanUserConversations(Long userId);
}
