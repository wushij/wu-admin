package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.admin.server.modules.ai.providers.AiProviderFactory;
import com.admin.server.modules.ai.providers.AiStreamListener;
import com.admin.server.modules.ai.service.AiChatLogService;
import com.admin.server.modules.ai.service.AiChatService;
import com.admin.server.modules.ai.service.AiModelService;
import com.admin.server.modules.ai.tool.AiToolContext;
import com.admin.server.modules.ai.tool.AiToolExecutor;
import com.admin.server.modules.ai.tool.ToolInvocationResult;
import com.admin.server.modules.ai.util.AiSanitizerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    /** 单轮对话最长上下文条数与单条内容长度限制（成本防护） */
    private static final int MAX_CONTEXT_MESSAGES = 20;
    private static final int MAX_CONTENT_LENGTH = 4000;

    /** 流式对话专用线程池（SSE 长任务，不占用 Web 容器线程） */
    private final ExecutorService chatExecutor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNamePrefix("ai-chat-stream-").setDaemon(true).build());

    @Resource
    private AiModelService aiModelService;

    @Resource
    private AiChatLogService aiChatLogService;

    @Resource
    private AiProviderFactory aiProviderFactory;

    @Resource
    private PromptAssembler promptAssembler;

    @Resource
    private AiToolExecutor aiToolExecutor;

    @PreDestroy
    public void shutdown() {
        chatExecutor.shutdownNow();
    }

    @Override
    public SseEmitter stream(AiChatStreamReqVO reqVO, Long userId, String username) {
        // SSE 无超时（长回答场景），由客户端断开或流结束终止
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(e -> cancelled.set(true));

        List<AiChatStreamReqVO.ChatMessage> messages;
        AiModelDO model;
        try {
            messages = sanitizeMessages(reqVO.getMessages());
            model = aiModelService.getAvailableModel(reqVO.getModelId());
        } catch (BusinessException e) {
            sendErrorQuietly(emitter, e.getMessage());
            return emitter;
        }

        // L1 知识注入：服务端组装 system 消息插入首位（sanitize 已丢弃客户端伪造的 system 角色）；
        // 组装失败不阻断对话，Provider 会回退内置人设提示词
        try {
            String latestUserQuery = extractLatestUserQuery(messages);
            String systemPrompt = promptAssembler.assemble(userId, model.getSystemPrompt(), reqVO.getSource(), latestUserQuery);
            if (StrUtil.isNotBlank(systemPrompt)) {
                AiChatStreamReqVO.ChatMessage systemMsg = new AiChatStreamReqVO.ChatMessage();
                systemMsg.setRole("system");
                systemMsg.setContent(systemPrompt);
                messages.add(0, systemMsg);
            }
        } catch (Exception e) {
            log.warn("AI system 提示词组装失败，降级为内置人设: {}", e.getMessage());
        }

        String plainApiKey = aiModelService.decryptApiKey(model);
        long startTime = System.currentTimeMillis();
        StringBuilder answerBuffer = new StringBuilder();

        // L3 工具：按用户角色可见性过滤 tool 声明；身份上下文从会话注入，工具轨迹用于审计
        final String toolSpecs = aiToolExecutor.buildToolSpecs(userId);
        final AiToolContext toolCtx = new AiToolContext(userId, username);
        final JSONArray toolTrace = new JSONArray();

        chatExecutor.submit(() -> {
            AiStreamListener listener = new AiStreamListener() {
                @Override
                public void onDelta(String delta) {
                    answerBuffer.append(delta);
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(delta));
                    } catch (IOException | IllegalStateException e) {
                        // 客户端已断开，标记取消让 Provider 停止拉流
                        cancelled.set(true);
                    }
                }

                @Override
                public void onComplete(int promptTokens, int completionTokens) {
                    long duration = System.currentTimeMillis() - startTime;
                    boolean aborted = cancelled.get();
                    if (!aborted) {
                        try {
                            JSONObject done = new JSONObject()
                                    .set("promptTokens", promptTokens)
                                    .set("completionTokens", completionTokens)
                                    .set("totalTokens", promptTokens + completionTokens)
                                    .set("durationMs", duration);
                            emitter.send(SseEmitter.event().name("done").data(done.toString()));
                            emitter.complete();
                        } catch (IOException | IllegalStateException e) {
                            cancelled.set(true);
                        }
                    }
                    saveChatLog(reqVO, model, userId, username, messages, answerBuffer.toString(),
                            promptTokens, completionTokens, duration, cancelled.get() ? 2 : 1, null, toolTrace);
                }

                @Override
                public boolean isCancelled() {
                    return cancelled.get();
                }

                @Override
                public boolean toolsEnabled() {
                    return StrUtil.isNotBlank(toolSpecs);
                }

                @Override
                public String toolSpecsJson() {
                    return toolSpecs;
                }

                @Override
                public String executeTool(String toolName, String argsJson) {
                    ToolInvocationResult r = aiToolExecutor.invoke(toolName, argsJson, toolCtx);
                    toolTrace.add(new JSONObject()
                            .set("tool", toolName)
                            .set("denied", r.isDenied())
                            .set("durationMs", r.getDurationMs()));
                    return r.getResult();
                }

                @Override
                public void onToolStatus(String toolName) {
                    if (cancelled.get()) {
                        return;
                    }
                    try {
                        JSONObject status = new JSONObject()
                                .set("tool", toolName)
                                .set("message", toolStatusMessage(toolName));
                        emitter.send(SseEmitter.event().name("status").data(status.toString()));
                    } catch (IOException | IllegalStateException e) {
                        cancelled.set(true);
                    }
                }
            };

            try {
                aiProviderFactory.getStrategy(model.getProvider()).streamChat(model, plainApiKey, messages, listener);
            } catch (Exception e) {
                log.error("AI 流式对话失败 provider={} model={}: {}", model.getProvider(), model.getModelName(), e.getMessage());
                String errMsg = e instanceof BusinessException ? e.getMessage() : "AI 服务暂时不可用，请稍后再试";
                sendErrorQuietly(emitter, errMsg);
                saveChatLog(reqVO, model, userId, username, messages, answerBuffer.toString(),
                        0, 0, System.currentTimeMillis() - startTime, 0, StrUtil.brief(e.getMessage(), 480), toolTrace);
            }
        });
        return emitter;
    }

    /** 校验并脱敏消息上下文 */
    private List<AiChatStreamReqVO.ChatMessage> sanitizeMessages(List<AiChatStreamReqVO.ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        List<AiChatStreamReqVO.ChatMessage> result = new ArrayList<>();
        int from = Math.max(0, messages.size() - MAX_CONTEXT_MESSAGES);
        for (int i = from; i < messages.size(); i++) {
            AiChatStreamReqVO.ChatMessage msg = messages.get(i);
            if (msg == null || StrUtil.isBlank(msg.getContent())) {
                continue;
            }
            String role = "assistant".equals(msg.getRole()) ? "assistant" : "user";
            AiChatStreamReqVO.ChatMessage clean = new AiChatStreamReqVO.ChatMessage();
            clean.setRole(role);
            clean.setContent(AiSanitizerUtil.sanitize(StrUtil.brief(msg.getContent(), MAX_CONTENT_LENGTH)));
            result.add(clean);
        }
        if (result.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        return result;
    }

    /** 取本轮最后一条用户提问（用于 L2 知识库检索） */
    private String extractLatestUserQuery(List<AiChatStreamReqVO.ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatStreamReqVO.ChatMessage msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return "";
    }

    /** 工具执行前的前端提示文案 */
    private String toolStatusMessage(String toolName) {
        if (toolName == null) {
            return "⚡ 正在查询系统数据...";
        }
        switch (toolName) {
            case "get_online_user_count":
                return "⚡ 正在查询在线用户数...";
            case "get_login_stat":
                return "⚡ 正在统计登录数据...";
            case "get_my_todo_count":
                return "⚡ 正在查询您的待办...";
            case "get_announce_latest":
                return "⚡ 正在获取最新公告...";
            default:
                return "⚡ 正在查询系统数据...";
        }
    }

    private void sendErrorQuietly(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(StrUtil.nullToEmpty(message)));
            emitter.complete();
        } catch (IOException | IllegalStateException ignored) {
            // 客户端已断开，忽略
        }
    }

    private void saveChatLog(AiChatStreamReqVO reqVO, AiModelDO model, Long userId, String username,
                             List<AiChatStreamReqVO.ChatMessage> messages, String answer,
                             int promptTokens, int completionTokens, long durationMs,
                             int chatStatus, String errorMsg, JSONArray toolTrace) {
        AiChatLogDO chatLog = new AiChatLogDO();
        chatLog.setUserId(userId);
        chatLog.setUsername(StrUtil.nullToEmpty(username));
        chatLog.setConversationId(StrUtil.nullToEmpty(reqVO.getConversationId()));
        chatLog.setModelId(model.getId());
        chatLog.setProvider(model.getProvider());
        chatLog.setModelName(model.getModelName());
        // 记录本轮最后一条用户提问（已脱敏）
        String question = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                question = messages.get(i).getContent();
                break;
            }
        }
        chatLog.setQuestion(question);
        chatLog.setAnswer(answer);
        chatLog.setPromptTokens(promptTokens);
        chatLog.setCompletionTokens(completionTokens);
        chatLog.setTotalTokens(promptTokens + completionTokens);
        chatLog.setDurationMs(durationMs);
        chatLog.setChatStatus(chatStatus);
        chatLog.setErrorMsg(errorMsg);
        chatLog.setSource("mobile".equalsIgnoreCase(reqVO.getSource()) ? "mobile" : "pc");
        chatLog.setToolTrace(toolTrace == null || toolTrace.isEmpty() ? null : toolTrace.toString());
        chatLog.setCreator(StrUtil.nullToEmpty(username));
        chatLog.setUpdater(StrUtil.nullToEmpty(username));
        aiChatLogService.saveLogAsync(chatLog);
    }
}
