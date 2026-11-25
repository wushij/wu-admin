package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.admin.server.modules.ai.providers.AiProviderFactory;
import com.admin.server.modules.ai.providers.AiStreamListener;
import com.admin.server.modules.ai.service.AiChatLogService;
import com.admin.server.modules.ai.service.AiChatAttachmentService;
import com.admin.server.modules.ai.service.AiChatService;
import com.admin.server.modules.ai.service.AiModelService;
import com.admin.server.modules.ai.service.search.WebSearchService;
import com.admin.server.modules.ai.service.search.vo.WebSearchCitationVO;
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
import java.util.concurrent.atomic.AtomicLong;

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
    private AiChatAttachmentService aiChatAttachmentService;

    @Resource
    private AiProviderFactory aiProviderFactory;

    @Resource
    private PromptAssembler promptAssembler;

    @Resource
    private AiToolExecutor aiToolExecutor;

    @Resource
    private AiChatQuotaGuard aiChatQuotaGuard;

    @Resource
    private WebSearchService webSearchService;

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

        // 用户级限流与 token 配额（fail-open）：被拒经 SSE error 下发，不落 chat log（未产生上游成本）
        String quotaErr = aiChatQuotaGuard.check(userId);
        if (quotaErr != null) {
            log.warn("AI 对话被限流拒绝 user={}: {}", userId, quotaErr);
            sendErrorQuietly(emitter, quotaErr);
            return emitter;
        }

        // L1 知识注入：服务端组装 system 消息插入首位（sanitize 已丢弃客户端伪造的 system 角色）；
        // 组装失败不阻断对话，Provider 会回退内置人设提示词
        final String latestUserQuery = extractLatestUserQuery(messages);

        // 本轮附件：正文注入最后一条用户消息，图片交给 Provider 做多模态输入；
        // 同时保留正文快照与元数据，随对话落库供历史会话回放。
        String attachmentContext = "";
        String attachmentsJson = "";
        AiChatStreamReqVO.ChatMessage latestUserMsg = extractLatestUserMessage(messages);
        if (latestUserMsg != null && latestUserMsg.getAttachmentIds() != null
                && !latestUserMsg.getAttachmentIds().isEmpty()) {
            try {
                attachmentContext = aiChatAttachmentService.buildContext(latestUserMsg.getAttachmentIds());
                if (StrUtil.isNotBlank(attachmentContext)) {
                    String base = StrUtil.nullToEmpty(latestUserMsg.getContent());
                    latestUserMsg.setContent(StrUtil.isBlank(base) ? attachmentContext : base + "\n\n" + attachmentContext);
                }
                latestUserMsg.setImages(aiChatAttachmentService.resolveImageUrls(latestUserMsg.getAttachmentIds()));
                if (latestUserMsg.getAttachments() != null && !latestUserMsg.getAttachments().isEmpty()) {
                    attachmentsJson = JSONUtil.toJsonStr(latestUserMsg.getAttachments());
                }
            } catch (Exception e) {
                log.warn("AI 附件上下文组装失败，已降级为纯文本问答: {}", e.getMessage());
            }
        }
        final String attachmentContextSnapshot = attachmentContext;
        final String attachmentsSnapshot = attachmentsJson;
        final boolean webSearchEnabled = Boolean.TRUE.equals(reqVO.getWebSearch()) && webSearchService.isCapabilityEnabled();
        try {
            String systemPrompt = promptAssembler.assemble(
                    userId, model.getSystemPrompt(), reqVO.getSource(), latestUserQuery, webSearchEnabled);
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
        AtomicLong firstDeltaAt = new AtomicLong(0L);
        StringBuilder answerBuffer = new StringBuilder();
        StringBuilder reasoningBuffer = new StringBuilder();

        // L3 工具：按用户角色权限与会话上下文（含联网开关）动态裁剪 tool 声明
        final AiToolContext toolCtx = new AiToolContext(userId, username, webSearchEnabled);
        final String toolSpecs = aiToolExecutor.buildToolSpecs(toolCtx);
        final JSONArray toolTrace = new JSONArray();
        // 本轮联网检索参考来源快照（流式期间由 web_search 工具动态补充，并在结束时持久化）
        final List<WebSearchCitationVO> citationsSnapshot = new ArrayList<>();

        chatExecutor.submit(() -> {
            AiStreamListener listener = new AiStreamListener() {
                @Override
                public void onDelta(String delta) {
                    if (StrUtil.isNotEmpty(delta)) {
                        firstDeltaAt.compareAndSet(0L, System.currentTimeMillis());
                    }
                    answerBuffer.append(delta);
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(delta));
                    } catch (IOException | IllegalStateException e) {
                        // 客户端已断开，标记取消让 Provider 停止拉流
                        cancelled.set(true);
                    }
                }

                @Override
                public void onReasoning(String delta) {
                    if (cancelled.get() || delta == null || delta.isEmpty()) {
                        return;
                    }
                    reasoningBuffer.append(delta);
                    try {
                        emitter.send(SseEmitter.event().name("reasoning").data(delta));
                    } catch (IOException | IllegalStateException e) {
                        cancelled.set(true);
                    }
                }

                @Override
                public void onComplete(int promptTokens, int completionTokens) {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    long thinkingDurationMs = resolveThinkingDurationMs(
                            startTime, firstDeltaAt.get(), endTime, reasoningBuffer.length(), toolTrace);
                    boolean aborted = cancelled.get();
                    // 先落库拿到记录ID，随 done 事件回传前端，便于按轮删除同步移除上下文
                    Long logId = saveChatLog(reqVO, model, userId, username, messages, answerBuffer.toString(),
                            reasoningBuffer.toString(), thinkingDurationMs,
                            promptTokens, completionTokens, duration, aborted ? 2 : 1, null, toolTrace,
                            attachmentContextSnapshot, attachmentsSnapshot, citationsSnapshot);
                    if (!aborted) {
                        try {
                            JSONObject done = new JSONObject()
                                    .set("promptTokens", promptTokens)
                                    .set("completionTokens", completionTokens)
                                    .set("totalTokens", promptTokens + completionTokens)
                                    .set("durationMs", duration)
                                    .set("logId", logId);
                            emitter.send(SseEmitter.event().name("done").data(done.toString()));
                            emitter.complete();
                        } catch (IOException | IllegalStateException e) {
                            cancelled.set(true);
                        }
                    }
                    // token 用量回写配额计数（成功与中断均回写，中断同样产生上游消耗）；
                    // 供应商未返回 usage 时按字符数估算，避免配额形同虚设
                    long tokensForQuota = resolveTokensForQuota(promptTokens, completionTokens, messages, answerBuffer.toString());
                    aiChatQuotaGuard.recordUsage(userId, tokensForQuota);
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
                    if ("web_search".equals(toolName) && !r.isDenied() && !cancelled.get()) {
                        List<WebSearchCitationVO> latestCitations = toolCtx.getCitationsSnapshot();
                        if (!latestCitations.isEmpty()) {
                            synchronized (citationsSnapshot) {
                                citationsSnapshot.clear();
                                citationsSnapshot.addAll(latestCitations);
                            }
                            try {
                                emitter.send(SseEmitter.event().name("citations").data(JSONUtil.toJsonStr(latestCitations)));
                            } catch (IOException | IllegalStateException e) {
                                cancelled.set(true);
                            }
                        }
                    }
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
                if (cancelled.get()) {
                    return;
                }
                aiProviderFactory.getStrategy(model).streamChat(model, plainApiKey, messages, listener);
            } catch (Exception e) {
                log.error("AI 流式对话失败 provider={} model={}: {}", model.getProvider(), model.getModelName(), e.getMessage());
                String errMsg = e instanceof BusinessException ? e.getMessage() : "AI 服务暂时不可用，请稍后再试";
                sendErrorQuietly(emitter, errMsg);
                long endTime = System.currentTimeMillis();
                long thinkingDurationMs = resolveThinkingDurationMs(
                        startTime, firstDeltaAt.get(), endTime, reasoningBuffer.length(), toolTrace);
                saveChatLog(reqVO, model, userId, username, messages, answerBuffer.toString(),
                        reasoningBuffer.toString(), thinkingDurationMs,
                        0, 0, endTime - startTime, 0, StrUtil.brief(e.getMessage(), 480), toolTrace,
                        attachmentContextSnapshot, attachmentsSnapshot, citationsSnapshot);
            }
        });
        return emitter;
    }

    /** 提取当前优先级最高的首选搜索引擎名称，避免状态提示串接多个备选通道造成误解。 */
    private String resolvePrimaryEngineLabel(String summary) {
        if (StrUtil.isBlank(summary)) {
            return "联网引擎";
        }
        int sepIdx = summary.indexOf(" · ");
        return (sepIdx > 0 ? summary.substring(0, sepIdx) : summary).trim();
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
            if (msg == null) {
                continue;
            }
            boolean hasAttachments = msg.getAttachmentIds() != null && !msg.getAttachmentIds().isEmpty();
            // 纯附件提问允许 content 为空，仍需保留该条用户消息
            if (StrUtil.isBlank(msg.getContent()) && !hasAttachments) {
                continue;
            }
            String role = "assistant".equals(msg.getRole()) ? "assistant" : "user";
            AiChatStreamReqVO.ChatMessage clean = new AiChatStreamReqVO.ChatMessage();
            clean.setRole(role);
            clean.setContent(AiSanitizerUtil.sanitize(StrUtil.brief(StrUtil.nullToEmpty(msg.getContent()), MAX_CONTENT_LENGTH)));
            if ("user".equals(role)) {
                clean.setAttachmentIds(msg.getAttachmentIds());
                clean.setImages(msg.getImages());
                clean.setAttachments(msg.getAttachments());
            }
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

    /** 取本轮最后一条用户消息（用于追加附件正文与图片） */
    private AiChatStreamReqVO.ChatMessage extractLatestUserMessage(List<AiChatStreamReqVO.ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                return messages.get(i);
            }
        }
        return null;
    }

    /**
     * 计算用于配额计数的 token 数：供应商返回了 usage 直接用其和；
     * 未返回（promptTokens+completionTokens==0）时按 (各消息+回答字符数)/2 保守估算，避免配额形同虚设。
     */
    private long resolveTokensForQuota(int promptTokens, int completionTokens,
                                       List<AiChatStreamReqVO.ChatMessage> messages, String answer) {
        int total = promptTokens + completionTokens;
        if (total > 0) {
            return total;
        }
        long chars = 0;
        if (messages != null) {
            for (AiChatStreamReqVO.ChatMessage m : messages) {
                if (m != null && m.getContent() != null) {
                    chars += m.getContent().length();
                }
            }
        }
        if (answer != null) {
            chars += answer.length();
        }
        return chars <= 0 ? 0 : chars / 2;
    }

    /** 工具执行前的前端提示文案 */
    private String toolStatusMessage(String toolName) {
        if (toolName == null) {
            return "正在查询系统数据";
        }
        switch (toolName) {
            case "get_online_user_count":
                return "正在查询在线用户数";
            case "get_login_stat":
                return "正在统计登录数据";
            case "get_my_todo_count":
                return "正在查询您的待办";
            case "get_announce_latest":
                return "正在获取最新公告";
            case "web_search":
                return resolveWebSearchStatusText();
            default:
                return "正在查询系统数据";
        }
    }

    private String resolveWebSearchStatusText() {
        try {
            String primaryEngine = resolvePrimaryEngineLabel(webSearchService.resolveActiveSummary());
            return (primaryEngine.contains("联网") || primaryEngine.contains("检索"))
                    ? "正在通过 " + primaryEngine + " 获取最新资料..."
                    : "正在通过 " + primaryEngine + " 联网检索最新资料...";
        } catch (Exception ignored) {
            return "正在联网检索最新资料...";
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

    private long resolveThinkingDurationMs(long startTime, long firstDeltaAt, long endTime,
                                           int reasoningLength, JSONArray toolTrace) {
        boolean hasThinking = reasoningLength > 0 || (toolTrace != null && !toolTrace.isEmpty());
        if (!hasThinking) {
            return 0L;
        }
        long finishAt = firstDeltaAt > 0 ? firstDeltaAt : endTime;
        return Math.max(0L, finishAt - startTime);
    }

    private Long saveChatLog(AiChatStreamReqVO reqVO, AiModelDO model, Long userId, String username,
                             List<AiChatStreamReqVO.ChatMessage> messages, String answer,
                             String reasoning, long thinkingDurationMs,
                             int promptTokens, int completionTokens, long durationMs,
                             int chatStatus, String errorMsg, JSONArray toolTrace,
                             String attachmentContext, String attachmentsJson,
                             List<WebSearchCitationVO> citations) {
        AiChatLogDO chatLog = new AiChatLogDO();
        chatLog.setUserId(userId);
        chatLog.setUsername(StrUtil.nullToEmpty(username));
        chatLog.setConversationId(StrUtil.nullToEmpty(reqVO.getConversationId()));
        chatLog.setModelId(model.getId());
        chatLog.setProvider(model.getProvider());
        chatLog.setModelName(model.getModelName());
        // 记录本轮最后一条用户提问（已脱敏）；剔除追加的附件正文，避免历史回放时正文重复展示
        String question = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                question = StrUtil.nullToEmpty(messages.get(i).getContent());
                break;
            }
        }
        if (StrUtil.isNotBlank(attachmentContext) && question.endsWith(attachmentContext)) {
            question = question.substring(0, question.length() - attachmentContext.length()).trim();
        }
        chatLog.setQuestion(question);
        chatLog.setAnswer(answer);
        chatLog.setReasoning(StrUtil.emptyToNull(reasoning));
        chatLog.setThinkingDurationMs(thinkingDurationMs > 0 ? thinkingDurationMs : null);
        chatLog.setAttachmentsJson(StrUtil.emptyToNull(attachmentsJson));
        chatLog.setAttachmentContext(StrUtil.emptyToNull(attachmentContext));
        chatLog.setCitationsJson(citations == null || citations.isEmpty() ? null : JSONUtil.toJsonStr(citations));
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
        return aiChatLogService.saveLogSync(chatLog);
    }
}
