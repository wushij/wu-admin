package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.ai.api.vo.AiChatAttachmentVO;
import com.admin.server.modules.ai.api.vo.AiChatHistoryItemVO;
import com.admin.server.modules.ai.api.vo.AiConversationVO;
import com.admin.server.modules.ai.service.search.vo.WebSearchCitationVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.admin.server.modules.ai.dal.dataobject.AiConversationDO;
import com.admin.server.modules.ai.dal.mysql.AiChatLogMapper;
import com.admin.server.modules.ai.service.AiChatLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiChatLogServiceImpl extends ServiceImpl<AiChatLogMapper, AiChatLogDO> implements AiChatLogService {

    private static final Logger log = LoggerFactory.getLogger(AiChatLogServiceImpl.class);

    @Async
    @Override
    public void saveLogAsync(AiChatLogDO chatLog) {
        try {
            save(chatLog);
        } catch (Exception e) {
            log.error("AI 对话记录异步落库失败: {}", e.getMessage());
        }
    }

    @Override
    public Long saveLogSync(AiChatLogDO chatLog) {
        try {
            save(chatLog);
            return chatLog.getId();
        } catch (Exception e) {
            log.error("AI 对话记录落库失败: {}", e.getMessage());
            return null;
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
        List<AiConversationDO> doList = baseMapper.selectConversations(userId, MAX_CONVERSATIONS);
        List<AiConversationVO> list = new ArrayList<>(doList.size());
        // 标题兜底与长度收敛，转换为视图展示对象
        for (AiConversationDO item : doList) {
            AiConversationVO vo = new AiConversationVO();
            vo.setConversationId(item.getConversationId());
            vo.setTitle(StrUtil.brief(StrUtil.blankToDefault(item.getTitle(), "（无标题会话）"), 60));
            vo.setLastTime(item.getLastTime());
            vo.setMessageCount(item.getMessageCount());
            list.add(vo);
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
            item.setId(chatLog.getId());
            item.setQuestion(StrUtil.nullToEmpty(chatLog.getQuestion()));
            // 失败轮（chatStatus=0）回答与思考均不回放，仅保留提问便于用户重新发问
            boolean failed = chatLog.getChatStatus() != null && chatLog.getChatStatus() == 0;
            item.setAnswer(failed ? "" : StrUtil.nullToEmpty(chatLog.getAnswer()));
            item.setReasoning(failed ? "" : StrUtil.nullToEmpty(chatLog.getReasoning()));
            item.setThinkingDurationMs(failed ? null : chatLog.getThinkingDurationMs());
            item.setToolMessages(failed ? List.of() : parseToolMessages(chatLog.getToolTrace()));
            item.setAttachmentContext(chatLog.getAttachmentContext());
            item.setAttachments(parseAttachments(chatLog.getAttachmentsJson()));
            item.setCitations(failed ? List.of() : parseCitations(chatLog.getCitationsJson()));
            item.setCreateTime(chatLog.getCreateTime());
            result.add(item);
        }
        return result;
    }

    /** 将 toolTrace JSON 转换为可读的思考步骤标签（坏 JSON 安全降级为空列表） */
    private List<String> parseToolMessages(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item == null || Boolean.TRUE.equals(item.getBool("denied", false))) {
                    continue;
                }
                String label = formatToolHistoryLabel(item.getStr("tool"));
                if (StrUtil.isNotBlank(label) && !labels.contains(label)) {
                    labels.add(label);
                }
            }
            return labels;
        } catch (Exception e) {
            log.warn("AI 历史工具轨迹解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    private String formatToolHistoryLabel(String toolName) {
        if (StrUtil.isBlank(toolName)) {
            return null;
        }
        return switch (toolName) {
            case "web_search" -> "已完成联网检索";
            case "get_online_user_count" -> "已查询在线用户数";
            case "get_login_stat" -> "已统计登录数据";
            case "get_my_todo_count" -> "已查询待办数量";
            case "get_announce_latest" -> "已获取最新公告";
            default -> "已完成系统查询";
        };
    }

    /** 解析附件元数据 JSON，损坏时降级为空列表，避免历史恢复整体失败 */
    private List<AiChatAttachmentVO> parseAttachments(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, AiChatAttachmentVO.class);
        } catch (Exception e) {
            log.warn("AI 历史附件元数据解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** 解析联网检索参考来源 JSON，损坏时降级为空列表，避免历史恢复异常 */
    private List<WebSearchCitationVO> parseCitations(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, WebSearchCitationVO.class);
        } catch (Exception e) {
            log.warn("AI 历史参考来源解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void deleteConversation(Long userId, String conversationId) {
        if (userId == null || userId <= 0 || StrUtil.isBlank(conversationId)) {
            return;
        }
        remove(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getUserId, userId)
                .eq(AiChatLogDO::getConversationId, conversationId));
    }

    @Override
    public void cleanUserConversations(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        remove(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getUserId, userId));
    }

    @Override
    public boolean deleteMessageRound(Long userId, Long logId) {
        if (userId == null || userId <= 0 || logId == null || logId <= 0) {
            return false;
        }
        // 严格限定本人记录，防止越权删除他人对话
        return remove(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getId, logId)
                .eq(AiChatLogDO::getUserId, userId));
    }

    @Override
    public boolean deleteLastRound(Long userId, String conversationId) {
        if (userId == null || userId <= 0 || StrUtil.isBlank(conversationId)) {
            return false;
        }
        AiChatLogDO last = getOne(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getUserId, userId)
                .eq(AiChatLogDO::getConversationId, conversationId)
                .orderByDesc(AiChatLogDO::getId)
                .last("LIMIT 1"));
        if (last == null || last.getId() == null) {
            return false;
        }
        return removeById(last.getId());
    }
}
