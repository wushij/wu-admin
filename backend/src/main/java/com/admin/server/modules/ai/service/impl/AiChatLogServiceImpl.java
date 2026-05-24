package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiChatHistoryItemVO;
import com.admin.server.modules.ai.api.vo.AiChatLogPageReqVO;
import com.admin.server.modules.ai.api.vo.AiConversationVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.admin.server.modules.ai.dal.mysql.AiChatLogMapper;
import com.admin.server.modules.ai.service.AiChatLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiChatLogServiceImpl extends ServiceImpl<AiChatLogMapper, AiChatLogDO> implements AiChatLogService {

    private static final Logger log = LoggerFactory.getLogger(AiChatLogServiceImpl.class);

    @Override
    public PageResult<AiChatLogDO> page(AiChatLogPageReqVO reqVO) {
        Page<AiChatLogDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        LambdaQueryWrapper<AiChatLogDO> wrapper = new LambdaQueryWrapper<AiChatLogDO>()
                .like(StrUtil.isNotBlank(reqVO.getUsername()), AiChatLogDO::getUsername, reqVO.getUsername())
                .eq(StrUtil.isNotBlank(reqVO.getProvider()), AiChatLogDO::getProvider, reqVO.getProvider())
                .eq(reqVO.getChatStatus() != null, AiChatLogDO::getChatStatus, reqVO.getChatStatus())
                .eq(StrUtil.isNotBlank(reqVO.getConversationId()), AiChatLogDO::getConversationId, reqVO.getConversationId())
                .orderByDesc(AiChatLogDO::getId);
        Page<AiChatLogDO> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(404, "对话日志不存在");
        }
    }

    @Override
    public void cleanLogs() {
        remove(new LambdaQueryWrapper<>());
    }

    @Async
    @Override
    public void saveLogAsync(AiChatLogDO chatLog) {
        try {
            save(chatLog);
        } catch (Exception e) {
            log.error("AI 对话日志异步落库失败: {}", e.getMessage());
        }
    }

    /** 历史会话列表上限（悬浮窗展示用，非管理列表） */
    private static final int MAX_CONVERSATIONS = 30;
    /** 单会话恢复的最大问答轮数 */
    private static final int MAX_HISTORY_ROUNDS = 50;

    @Override
    public List<AiConversationVO> listConversations(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        List<AiConversationVO> list = baseMapper.selectConversations(userId, MAX_CONVERSATIONS);
        // 标题兜底与长度收敛
        for (AiConversationVO vo : list) {
            vo.setTitle(StrUtil.brief(StrUtil.blankToDefault(vo.getTitle(), "（无标题会话）"), 60));
        }
        return list;
    }

    @Override
    public List<AiChatHistoryItemVO> listHistory(Long userId, String conversationId) {
        if (userId == null || userId <= 0 || StrUtil.isBlank(conversationId)) {
            return List.of();
        }
        // 严格限定本人会话，防止用任意 conversationId 探测他人对话
        List<AiChatLogDO> logs = list(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getUserId, userId)
                .eq(AiChatLogDO::getConversationId, conversationId)
                .orderByAsc(AiChatLogDO::getId)
                .last("LIMIT " + MAX_HISTORY_ROUNDS));
        List<AiChatHistoryItemVO> result = new ArrayList<>(logs.size());
        for (AiChatLogDO chatLog : logs) {
            AiChatHistoryItemVO item = new AiChatHistoryItemVO();
            item.setQuestion(StrUtil.nullToEmpty(chatLog.getQuestion()));
            // 失败轮（chatStatus=0）回答不回放，仅保留提问便于用户重新发问
            boolean failed = chatLog.getChatStatus() != null && chatLog.getChatStatus() == 0;
            item.setAnswer(failed ? "" : StrUtil.nullToEmpty(chatLog.getAnswer()));
            item.setCreateTime(chatLog.getCreateTime());
            result.add(item);
        }
        return result;
    }
}
