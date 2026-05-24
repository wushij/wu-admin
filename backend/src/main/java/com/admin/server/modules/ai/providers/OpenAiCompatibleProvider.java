package com.admin.server.modules.ai.providers;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.admin.server.modules.ai.util.AiPromptTemplates;
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
import java.util.Set;
import java.util.TreeMap;

/**
 * OpenAI 兼容协议统一实现
 * <p>DeepSeek / OpenAI / Qwen / Kimi 均遵循 /chat/completions SSE 协议，仅 baseUrl 与 model 不同，共用此类。</p>
 */
@Component
public class OpenAiCompatibleProvider implements AiProviderStrategy {

    private static final Set<String> SUPPORTED = Set.of("deepseek", "openai", "qwen", "kimi");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public boolean supports(String provider) {
        return provider != null && SUPPORTED.contains(provider.toLowerCase());
    }

    /** 单轮对话最多允许的工具调用轮次（防模型死循环拉高成本，设计 §5.2） */
    private static final int MAX_TOOL_ROUNDS = 3;

    @Override
    public void streamChat(AiModelDO model, String plainApiKey,
                           List<AiChatStreamReqVO.ChatMessage> messages, AiStreamListener listener) throws Exception {
        JSONArray runningMessages = buildInitialMessages(model, messages);
        String toolSpecs = listener.toolsEnabled() ? listener.toolSpecsJson() : null;
        int totalPrompt = 0;
        int totalCompletion = 0;

        for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
            // 达到工具轮次上限后，最后一轮不再下发 tools，强制模型直接作答
            String roundToolSpecs = round < MAX_TOOL_ROUNDS ? toolSpecs : null;
            JSONObject body = buildStreamBody(model, runningMessages, roundToolSpecs);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatCompletionsUrl(model.getBaseUrl())))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + StrUtil.nullToEmpty(plainApiKey))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new BusinessException("大模型接口调用失败(HTTP " + response.statusCode() + "): " + readErrorBody(response.body()));
            }

            StreamRoundResult roundResult = parseStream(response, listener);
            totalPrompt += roundResult.promptTokens;
            totalCompletion += roundResult.completionTokens;

            // 模型请求调用工具：执行后回填消息进入下一轮；否则本轮即最终回答
            if (roundToolSpecs != null && !roundResult.toolCalls.isEmpty() && !listener.isCancelled()) {
                runningMessages.add(buildAssistantToolCallMessage(roundResult.toolCalls));
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

    /** 解析一轮 SSE 流：发射内容增量、累积 tool_calls 与 usage */
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
                JSONObject chunk = JSONUtil.parseObj(payload);
                JSONArray choices = chunk.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject delta = choice.getJSONObject("delta");
                    if (delta != null) {
                        String content = delta.getStr("content");
                        if (StrUtil.isNotEmpty(content)) {
                            listener.onDelta(content);
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

    private JSONObject buildAssistantToolCallMessage(List<ToolCallAcc> toolCalls) {
        JSONArray arr = new JSONArray();
        for (ToolCallAcc call : toolCalls) {
            arr.add(new JSONObject()
                    .set("id", StrUtil.nullToEmpty(call.id))
                    .set("type", "function")
                    .set("function", new JSONObject()
                            .set("name", StrUtil.nullToEmpty(call.name))
                            .set("arguments", call.args.length() == 0 ? "{}" : call.args.toString())));
        }
        return new JSONObject().set("role", "assistant").set("content", "").set("tool_calls", arr);
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
        final List<ToolCallAcc> toolCalls = new ArrayList<>();
    }

    @Override
    public long testConnection(AiModelDO model, String plainApiKey) throws Exception {
        JSONObject body = new JSONObject();
        body.set("model", model.getModelName());
        body.set("stream", false);
        body.set("max_tokens", 1);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "user").set("content", "ping"));
        body.set("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chatCompletionsUrl(model.getBaseUrl())))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + StrUtil.nullToEmpty(plainApiKey))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        long start = System.currentTimeMillis();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long latency = System.currentTimeMillis() - start;
        if (response.statusCode() != 200) {
            throw new BusinessException("连通性测试失败(HTTP " + response.statusCode() + "): " + StrUtil.brief(response.body(), 300));
        }
        return latency;
    }

    /** baseUrl 智能补全 /chat/completions 后缀 */
    private String chatCompletionsUrl(String baseUrl) {
        String url = StrUtil.removeSuffix(StrUtil.trim(baseUrl), "/");
        if (url.endsWith("/chat/completions")) {
            return url;
        }
        return url + "/chat/completions";
    }

    private JSONObject buildStreamBody(AiModelDO model, JSONArray messagesArray, String toolSpecsJson) {
        JSONObject body = new JSONObject();
        body.set("model", model.getModelName());
        body.set("stream", true);
        if (model.getTemperature() != null) {
            body.set("temperature", model.getTemperature());
        }
        if (model.getMaxTokens() != null && model.getMaxTokens() > 0) {
            body.set("max_tokens", model.getMaxTokens());
        }
        // 请求供应商在最后一个 chunk 返回 usage 统计（OpenAI 兼容协议扩展，不支持的厂商会忽略）
        body.set("stream_options", new JSONObject().set("include_usage", true));
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

    private String readErrorBody(InputStream body) {
        try (InputStream is = body) {
            return StrUtil.brief(new String(is.readAllBytes(), StandardCharsets.UTF_8), 300);
        } catch (Exception e) {
            return "";
        }
    }
}
