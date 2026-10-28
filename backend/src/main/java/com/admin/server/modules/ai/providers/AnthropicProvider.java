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
 * Anthropic Messages 协议实现。
 * <p>覆盖 Claude 官方接口（{@code POST /v1/messages}）以及兼容该协议的网关。
 * 与 OpenAI 兼容协议的主要差异：鉴权用 {@code x-api-key}、必须带
 * {@code anthropic-version}、system 放在顶层、工具调用使用 content block
 * （tool_use / tool_result）而不是 tool_calls。</p>
 */
@Component
public class AnthropicProvider implements AiProviderStrategy {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);

    private static final String PROTOCOL = "anthropic";

    private static final String DEFAULT_SUFFIX = "/v1/messages";

    /** Anthropic 要求的协议版本头，2023-06-01 为长期稳定版本 */
    private static final String ANTHROPIC_VERSION = ProviderHttpUtils.ANTHROPIC_VERSION;

    /** max_tokens 在 Anthropic 协议中必填，配置缺失时使用的兜底值 */
    private static final int DEFAULT_MAX_TOKENS = 8192;

    private static final int MAX_TOOL_ROUNDS = 3;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public String protocol() {
        return PROTOCOL;
    }

    @Override
    public void streamChat(AiModelDO model, String plainApiKey,
                           List<AiChatStreamReqVO.ChatMessage> messages, AiStreamListener listener) throws Exception {
        String systemPrompt = resolveSystemPrompt(model, messages);
        JSONArray runningMessages = convertInitialMessages(messages);
        String toolSpecs = listener.toolsEnabled() ? listener.toolSpecsJson() : null;
        int totalPrompt = 0;
        int totalCompletion = 0;

        for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
            // 达到工具轮次上限后不再下发 tools，强制模型直接作答
            String roundToolSpecs = round < MAX_TOOL_ROUNDS ? toolSpecs : null;
            JSONObject body = buildStreamBody(model, runningMessages, systemPrompt, roundToolSpecs);
            HttpResponse<InputStream> response = send(model, plainApiKey, body);

            StreamRoundResult roundResult = parseStream(response, listener);
            totalPrompt += roundResult.promptTokens;
            totalCompletion += roundResult.completionTokens;

            if (roundToolSpecs != null && !roundResult.toolUses.isEmpty() && !listener.isCancelled()) {
                // Anthropic 要求把模型本轮输出（文本 + tool_use）原样回填，再追加一条 user 的 tool_result
                runningMessages.add(buildAssistantToolUseMessage(roundResult));
                runningMessages.add(buildToolResultMessage(roundResult, listener));
                continue;
            }
            break;
        }
        listener.onComplete(totalPrompt, totalCompletion);
    }

    /** 系统提示词：优先取服务层组装的消息，其次模型自定义，最后内置人设 */
    private String resolveSystemPrompt(AiModelDO model, List<AiChatStreamReqVO.ChatMessage> messages) {
        for (AiChatStreamReqVO.ChatMessage message : messages) {
            if ("system".equals(message.getRole()) && StrUtil.isNotBlank(message.getContent())) {
                return message.getContent();
            }
        }
        return StrUtil.blankToDefault(model.getSystemPrompt(), AiPromptTemplates.PERSONA);
    }

    /** 转换为 Anthropic 的 messages：只保留 user/assistant，system 已提升到顶层 */
    private JSONArray convertInitialMessages(List<AiChatStreamReqVO.ChatMessage> messages) {
        JSONArray result = new JSONArray();
        for (AiChatStreamReqVO.ChatMessage message : messages) {
            if ("system".equals(message.getRole())) {
                continue;
            }
            String role = "assistant".equals(message.getRole()) ? "assistant" : "user";
            result.add(new JSONObject().set("role", role).set("content", StrUtil.nullToEmpty(message.getContent())));
        }
        if (result.isEmpty()) {
            result.add(new JSONObject().set("role", "user").set("content", "你好"));
        }
        return result;
    }

    private JSONObject buildStreamBody(AiModelDO model, JSONArray messages,
                                       String systemPrompt, String toolSpecsJson) {
        JSONObject body = new JSONObject();
        body.set("model", model.getModelName());
        body.set("stream", true);
        // Anthropic 的 max_tokens 必填
        body.set("max_tokens", model.getMaxTokens() != null && model.getMaxTokens() > 0
                ? model.getMaxTokens() : DEFAULT_MAX_TOKENS);
        if (model.getTemperature() != null) {
            // 平台侧允许 0~2，Anthropic 只接受 0~1，超出范围会直接 400
            double temperature = Math.max(0D, Math.min(1D, model.getTemperature()));
            body.set("temperature", temperature);
        }
        if (StrUtil.isNotBlank(systemPrompt)) {
            body.set("system", systemPrompt);
        }
        if (StrUtil.isNotBlank(toolSpecsJson)) {
            JSONArray tools = convertTools(toolSpecsJson);
            if (!tools.isEmpty()) {
                body.set("tools", tools);
                body.set("tool_choice", new JSONObject().set("type", "auto"));
            }
        }
        body.set("messages", messages);
        return body;
    }

    /** OpenAI tools 声明 → Anthropic tools 声明（function.parameters → input_schema） */
    private JSONArray convertTools(String toolSpecsJson) {
        JSONArray result = new JSONArray();
        JSONArray openAiTools;
        try {
            openAiTools = JSONUtil.parseArray(toolSpecsJson);
        } catch (Exception e) {
            log.warn("AI 工具声明解析失败，本轮按无工具处理: {}", e.getMessage());
            return result;
        }
        for (Object item : openAiTools) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject function = ((JSONObject) item).getJSONObject("function");
            if (function == null || StrUtil.isBlank(function.getStr("name"))) {
                continue;
            }
            JSONObject schema = function.getJSONObject("parameters");
            if (schema == null) {
                schema = new JSONObject().set("type", "object").set("properties", new JSONObject());
            }
            result.add(new JSONObject()
                    .set("name", function.getStr("name"))
                    .set("description", StrUtil.nullToEmpty(function.getStr("description")))
                    .set("input_schema", schema));
        }
        return result;
    }

    private HttpResponse<InputStream> send(AiModelDO model, String plainApiKey, JSONObject body) throws Exception {
        String url = ProviderHttpUtils.resolveUrl(model.getBaseUrl(), model.getPathTemplate(),
                model.getModelName(), DEFAULT_SUFFIX);
        HttpRequest request = buildRequest(url, model, plainApiKey, Duration.ofMinutes(5), body);
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw ProviderHttpUtils.httpError(response.statusCode(),
                    ProviderHttpUtils.readErrorBody(response.body()));
        }
        return response;
    }

    private HttpRequest buildRequest(String url, AiModelDO model, String plainApiKey,
                                     Duration timeout, JSONObject body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (StrUtil.isNotBlank(plainApiKey)) {
            builder.header("x-api-key", plainApiKey);
        }
        ProviderHttpUtils.applyExtraHeaders(builder, model.getExtraHeaders());
        return builder.build();
    }

    /** 解析一轮 SSE 流：text_delta / input_json_delta / usage / error */
    private StreamRoundResult parseStream(HttpResponse<InputStream> response, AiStreamListener listener) throws Exception {
        StreamRoundResult result = new StreamRoundResult();
        Map<Integer, ToolUseAcc> toolUseMap = new TreeMap<>();
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
                    continue;
                }
                String type = chunk.getStr("type");
                if ("error".equals(type)) {
                    throw new BusinessException("大模型返回错误: "
                            + ProviderHttpUtils.extractErrorMessage(chunk.get("error")));
                }
                if ("message_start".equals(type)) {
                    JSONObject message = chunk.getJSONObject("message");
                    JSONObject usage = message == null ? null : message.getJSONObject("usage");
                    if (usage != null) {
                        result.promptTokens = usage.getInt("input_tokens", 0);
                    }
                } else if ("content_block_start".equals(type)) {
                    JSONObject block = chunk.getJSONObject("content_block");
                    if (block != null && "tool_use".equals(block.getStr("type"))) {
                        ToolUseAcc acc = new ToolUseAcc();
                        acc.id = block.getStr("id");
                        acc.name = block.getStr("name");
                        toolUseMap.put(chunk.getInt("index", toolUseMap.size()), acc);
                    }
                } else if ("content_block_delta".equals(type)) {
                    JSONObject delta = chunk.getJSONObject("delta");
                    if (delta == null) {
                        continue;
                    }
                    String deltaType = delta.getStr("type");
                    if ("text_delta".equals(deltaType)) {
                        String text = delta.getStr("text");
                        if (StrUtil.isNotEmpty(text)) {
                            result.text.append(text);
                            listener.onDelta(text);
                        }
                    } else if ("input_json_delta".equals(deltaType)) {
                        ToolUseAcc acc = toolUseMap.get(chunk.getInt("index", 0));
                        if (acc != null && delta.getStr("partial_json") != null) {
                            acc.args.append(delta.getStr("partial_json"));
                        }
                    }
                } else if ("message_delta".equals(type)) {
                    JSONObject usage = chunk.getJSONObject("usage");
                    if (usage != null) {
                        result.completionTokens = usage.getInt("output_tokens", 0);
                    }
                }
            }
        }
        result.toolUses.addAll(toolUseMap.values());
        return result;
    }

    private JSONObject buildAssistantToolUseMessage(StreamRoundResult roundResult) {
        JSONArray content = new JSONArray();
        if (StrUtil.isNotEmpty(roundResult.text)) {
            content.add(new JSONObject().set("type", "text").set("text", roundResult.text.toString()));
        }
        for (ToolUseAcc call : roundResult.toolUses) {
            content.add(new JSONObject()
                    .set("type", "tool_use")
                    .set("id", StrUtil.nullToEmpty(call.id))
                    .set("name", StrUtil.nullToEmpty(call.name))
                    .set("input", parseToolInput(call.args.toString())));
        }
        return new JSONObject().set("role", "assistant").set("content", content);
    }

    private JSONObject buildToolResultMessage(StreamRoundResult roundResult, AiStreamListener listener) {
        JSONArray content = new JSONArray();
        for (ToolUseAcc call : roundResult.toolUses) {
            listener.onToolStatus(call.name);
            String result = listener.executeTool(call.name,
                    call.args.length() == 0 ? "{}" : call.args.toString());
            content.add(new JSONObject()
                    .set("type", "tool_result")
                    .set("tool_use_id", StrUtil.nullToEmpty(call.id))
                    .set("content", StrUtil.nullToEmpty(result)));
        }
        return new JSONObject().set("role", "user").set("content", content);
    }

    private JSONObject parseToolInput(String args) {
        if (StrUtil.isBlank(args)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(args);
        } catch (Exception e) {
            log.warn("工具调用参数不是合法 JSON，按空对象回传: {}", StrUtil.brief(args, 120));
            return new JSONObject();
        }
    }

    @Override
    public long testConnection(AiModelDO model, String plainApiKey) throws Exception {
        JSONObject body = new JSONObject();
        body.set("model", model.getModelName());
        body.set("stream", false);
        body.set("max_tokens", 16);
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

    /** 单轮流解析结果 */
    private static class StreamRoundResult {
        int promptTokens;
        int completionTokens;
        final StringBuilder text = new StringBuilder();
        final List<ToolUseAcc> toolUses = new ArrayList<>();
    }

    /** tool_use content block 累积器 */
    private static class ToolUseAcc {
        String id;
        String name;
        final StringBuilder args = new StringBuilder();
    }
}
