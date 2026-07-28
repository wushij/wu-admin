package com.admin.server.modules.ai.service;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.ai.api.vo.AiChatLogPageReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;

/**
 * AI 对话日志审计
 */
public interface AiChatLogService {

    PageResult<AiChatLogDO> page(AiChatLogPageReqVO reqVO);

    void delete(Long id);

    void cleanLogs();

    /** 异步落库对话日志 */
    void saveLogAsync(AiChatLogDO chatLog);
}
