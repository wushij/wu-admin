package com.admin.server.modules.ai.providers;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.admin.server.modules.ai.util.AiPromptTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAI 兼容协议实现。
 * <p>DeepSeek / Kimi / GLM / 通义 / 豆包 / 千帆 / 混元 / MiniMax / SiliconFlow /
 * OpenRouter / Groq / xAI / Ollama / vLLM / One-API 等均遵循
 * {@code POST {baseUrl}/chat/completions} 的 SSE 协议，共用此类。</p>
 */
@Component
public class OpenAiCompatibleProvider implements AiProviderStrategy {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);

    private static final String PROTOCOL = "openai";

    private static final String DEFAULT_SUFFIX = "/chat/completions";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** 单轮对话最多允许的工具调用轮次（防模型死循环拉高成本，设计 §5.2） */
    private static final int MAX_TOOL_ROUNDS = 3;

    /** 兼容降级最多尝试次数：原始请求 → 替换 max_tokens → 去掉扩展参数 */
    private static final int MAX_COMPAT_ATTEMPTS = 3;

    @Override
    public String protocol() {
        return PROTOCOL;
    }

    @Override
    public void streamChat(AiModelDO model, String plainApiKey,
                           List<AiChatStreamReqVO.ChatMessage> messages, AiStreamListener listener) throws Exception {
        JSONArray runningMessages = buildInitialMessages(model, messages);
        String toolSpecs = listener.toolsEnabled() ? listener.toolSpecsJson() : null;
        CompatState state = new CompatState();
        int totalPrompt = 0;
        int totalCompletion = 0;

        for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
            // 达到工具轮次上限或已确认模型不支持 tools 后，不再下发 tools，强制模型直接作答
            String roundToolSpecs = (round < MAX_TOOL_ROUNDS && !state.toolsDisabled) ? toolSpecs : null;
            JSONObject body = buildStreamBody(model, state, runningMessages, roundToolSpecs);
            HttpResponse<InputStream> response = sendWithCompat(model, plainApiKey, state, body);

            StreamRoundResult roundResult = parseStream(response, listener);
            totalPrompt += roundResult.promptTokens;
            totalCompletion += roundResult.completionTokens;

            // 模型请求调用工具：执行后回填消息进入下一轮；否则本轮即最终回答
            if (roundToolSpecs != null && !roundResult.toolCalls.isEmpty() && !listener.isCancelled()) {
                runningMessages.add(buildAssistantToolCallMessage(roundResult.toolCalls, roundResult.reasoning.toString()));
                for (ToolCallAcc call : roundResult.toolCalls) {
                    listener.onToolStatus(call.name);
                    String result = listener.executeTool(call.name, call.args.toString());
                    runningMessages.add(new JSONObject()
                            .set("role", "tool")
                            .set("tool_call_id", StrUtil.nullToEmpty(call.id))
                            .set("content", StrUtil.nullToEmpty(result)));
                }
                continue;
            }
            break;
        }
        listener.onComplete(totalPrompt, totalCompletion);
    }

    /**
     * 发送对话请求，并在厂商拒绝扩展参数时自动降级重试。
     * <p>常见场景：模型不支持 tools / 不支持 stream_options.include_usage /
     * 新版 OpenAI 推理模型要求 max_completion_tokens 而不是 max_tokens。</p>
     */
    private HttpResponse<InputStream> sendWithCompat(AiModelDO model, String plainApiKey,
                                                     CompatState state, JSONObject body) throws Exception {
        String url = ProviderHttpUtils.resolveUrl(model.getBaseUrl(), model.getPathTemplate(),
                model.getModelName(), DEFAULT_SUFFIX);
        for (int attempt = 0; attempt < MAX_COMPAT_ATTEMPTS; attempt++) {
            HttpRequest request = buildRequest(url, model, plainApiKey, Duration.ofMinutes(5), body);
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                return response;
            }
            String errorBody = ProviderHttpUtils.readErrorBody(response.body());
            if (!degradeIfPossible(body, state, response.statusCode(), errorBody)) {
                throw ProviderHttpUtils.httpError(response.statusCode(), errorBody);
            }
            log.warn("OpenAI 兼容接口参数不被支持，已降级重试 model={} http={} toolsDisabled={} streamOptionsDisabled={} useCompletionTokens={} body={}",
                    model.getModelName(), response.statusCode(), state.toolsDisabled,
                    state.streamOptionsDisabled, state.useCompletionTokens, StrUtil.brief(errorBody, 200));
        }
        throw new BusinessException("大模型接口调用失败：兼容降级重试次数已用尽");
    }

    /** 根据错误信息调整请求体，返回是否发生了可重试的变化 */
    private boolean degradeIfPossible(JSONObject body, CompatState state, int statusCode, String errorBody) {
        // 新版 OpenAI 推理模型明确提示改用 max_completion_tokens，优先只改字段，不牺牲工具能力
        if (!state.useCompletionTokens && body.containsKey("max_tokens")
                && StrUtil.containsIgnoreCase(errorBody, "max_completion_tokens")) {
            body.set("max_completion_tokens", body.remove("max_tokens"));
            state.useCompletionTokens = true;
            return true;
        }
        if (statusCode != 400 && statusCode != 422) {
            return false;
        }
        boolean changed = false;
        if (!state.streamOptionsDisabled && body.containsKey("stream_options")) {
            body.remove("stream_options");
            state.streamOptionsDisabled = true;
            changed = true;
        }
        if (!state.toolsDisabled && body.containsKey("tools")) {
            body.remove("tools");
            body.remove("tool_choice");
            state.toolsDisabled = true;
            changed = true;
        }
        return changed;
    }

    /** 解析一轮 SSE 流：发射内容增量、累积 tool_calls / reasoning_content 与 usage */
    private StreamRoundResult parseStream(HttpResponse<InputStream> response, AiStreamListener listener) throws Exception {
        StreamRoundResult result = new StreamRoundResult();
        Map<Integer, ToolCallAcc> toolCallMap = new TreeMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (listener.isCancelled()) {
                    break;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                    continue;
                }
                JSONObject chunk;
                try {
                    chunk = JSONUtil.parseObj(payload);
                } catch (Exception e) {
                    // 少数网关会插入非 JSON 心跳，忽略即可
                    continue;
                }
                Object error = chunk.get("error");
                if (error != null) {
                    throw new BusinessException("大模型返回错误: " + ProviderHttpUtils.extractErrorMessage(error));
                }
                JSONArray choices = chunk.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject delta = choice.getJSONObject("delta");
                    if (delta != null) {
                        String content = delta.getStr("content");
                        if (StrUtil.isNotEmpty(content)) {
                            listener.onDelta(content);
                        }
                        // 推理模型（如 DeepSeek reasoner）的思维链，工具循环时需要回传
                        String reasoning = delta.getStr("reasoning_content");
                        if (reasoning != null) {
                            result.reasoning.append(reasoning);
                        }
                        accumulateToolCalls(delta.getJSONArray("tool_calls"), toolCallMap);
                    }
                }
                JSONObject usage = chunk.getJSONObject("usage");
                if (usage != null) {
                    result.promptTokens = usage.getInt("prompt_tokens", 0);
                    result.completionTokens = usage.getInt("completion_tokens", 0);
                }
            }
        }
        result.toolCalls.addAll(toolCallMap.values());
        return result;
    }

    /** 累积分片的 tool_calls（按 index 聚合 id/name/arguments） */
    private void accumulateToolCalls(JSONArray toolCalls, Map<Integer, ToolCallAcc> toolCallMap) {
        if (toolCalls == null) {
            return;
        }
        for (int i = 0; i < toolCalls.size(); i++) {
            JSONObject tc = toolCalls.getJSONObject(i);
            int index = tc.getInt("index", i);
            ToolCallAcc acc = toolCallMap.computeIfAbsent(index, k -> new ToolCallAcc());
            String id = tc.getStr("id");
            if (StrUtil.isNotBlank(id)) {
                acc.id = id;
            }
            JSONObject function = tc.getJSONObject("function");
            if (function != null) {
                String name = function.getStr("name");
                if (StrUtil.isNotBlank(name)) {
                    acc.name = name;
                }
                String argsFragment = function.getStr("arguments");
                if (argsFragment != null) {
                    acc.args.append(argsFragment);
                }
            }
        }
    }

    private JSONObject buildAssistantToolCallMessage(List<ToolCallAcc> toolCalls, String reasoning) {
        JSONArray arr = new JSONArray();
        for (ToolCallAcc call : toolCalls) {
            arr.add(new JSONObject()
                    .set("id", StrUtil.nullToEmpty(call.id))
                    .set("type", "function")
                    .set("function", new JSONObject()
                            .set("name", StrUtil.nullToEmpty(call.name))
                            .set("arguments", call.args.length() == 0 ? "{}" : call.args.toString())));
        }
        JSONObject message = new JSONObject().set("role", "assistant").set("content", "").set("tool_calls", arr);
        if (StrUtil.isNotBlank(reasoning)) {
            message.set("reasoning_content", reasoning);
        }
        return message;
    }

    /** tool_calls 分片累积器 */
    private static class ToolCallAcc {
        String id;
        String name;
        final StringBuilder args = new StringBuilder();
    }

    /** 单轮流解析结果 */
    private static class StreamRoundResult {
        int promptTokens;
        int completionTokens;
        final StringBuilder reasoning = new StringBuilder();
        final List<ToolCallAcc> toolCalls = new ArrayList<>();
    }

    /** 兼容降级状态，在一轮对话的多轮工具调用之间共享 */
    private static class CompatState {
        boolean toolsDisabled;
        boolean streamOptionsDisabled;
        boolean useCompletionTokens;
    }

    @Override
    public long testConnection(AiModelDO model, String plainApiKey) throws Exception {
        JSONObject body = new JSONObject();
        body.set("model", model.getModelName());
        body.set("stream", false);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "user").set("content", "ping"));
        body.set("messages", messages);

        String url = ProviderHttpUtils.resolveUrl(model.getBaseUrl(), model.getPathTemplate(),
                model.getModelName(), DEFAULT_SUFFIX);
        HttpRequest request = buildRequest(url, model, plainApiKey, Duration.ofSeconds(30), body);

        long start = System.currentTimeMillis();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long latency = System.currentTimeMillis() - start;
        if (response.statusCode() != 200) {
            throw ProviderHttpUtils.httpError(response.statusCode(), response.body());
        }
        return latency;
    }

    @Override
    public List<String> listModels(AiModelDO model, String plainApiKey) throws Exception {
        return ProviderHttpUtils.fetchModelIds(model.getBaseUrl(), PROTOCOL,
                model.getExtraHeaders(), plainApiKey);
    }

    private HttpRequest buildRequest(String url, AiModelDO model, String plainApiKey,
                                     Duration timeout, JSONObject body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + StrUtil.nullToEmpty(plainApiKey))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        ProviderHttpUtils.applyExtraHeaders(builder, model.getExtraHeaders());
        return builder.build();
    }

    private JSONObject buildStreamBody(AiModelDO model, CompatState state,
                                       JSONArray messagesArray, String toolSpecsJson) {
        JSONObject body = new JSONObject();
        body.set("model", model.getModelName());
        body.set("stream", true);
        if (model.getTemperature() != null) {
            body.set("temperature", model.getTemperature());
        }
        if (model.getMaxTokens() != null && model.getMaxTokens() > 0) {
            if (state.useCompletionTokens) {
                body.set("max_completion_tokens", model.getMaxTokens());
            } else {
                body.set("max_tokens", model.getMaxTokens());
            }
        }
        // 请求供应商在最后一个 chunk 返回 usage 统计；不支持的厂商会在收到 400 后自动去掉该字段重试
        if (!state.streamOptionsDisabled) {
            body.set("stream_options", new JSONObject().set("include_usage", true));
        }
        if (StrUtil.isNotBlank(toolSpecsJson)) {
            body.set("tools", JSONUtil.parseArray(toolSpecsJson));
            body.set("tool_choice", "auto");
        }
        body.set("messages", messagesArray);
        return body;
    }

    /** 构建初始消息数组（system + 多轮历史） */
    private JSONArray buildInitialMessages(AiModelDO model, List<AiChatStreamReqVO.ChatMessage> messages) {
        JSONArray msgArray = new JSONArray();
        // 服务层（PromptAssembler）已组装 system 消息时直接透传；
        // 未携带时回退「模型自定义 systemPrompt 或内置人设」（兜底链路，如旧调用方）
        boolean hasSystemMessage = !messages.isEmpty() && "system".equals(messages.get(0).getRole());
        if (!hasSystemMessage) {
            String systemPrompt = StrUtil.blankToDefault(model.getSystemPrompt(), AiPromptTemplates.PERSONA);
            msgArray.add(new JSONObject().set("role", "system").set("content", systemPrompt));
        }
        for (AiChatStreamReqVO.ChatMessage msg : messages) {
            msgArray.add(new JSONObject().set("role", msg.getRole()).set("content", msg.getContent()));
        }
        return msgArray;
    }
}
