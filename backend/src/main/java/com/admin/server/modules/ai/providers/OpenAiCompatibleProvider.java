package com.admin.server.modules.ai.providers;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
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
import java.util.List;
import java.util.Set;

/**
 * OpenAI 兼容协议统一实现
 * <p>DeepSeek / OpenAI / Qwen / Kimi 均遵循 /chat/completions SSE 协议，仅 baseUrl 与 model 不同，共用此类。</p>
 */
@Component
public class OpenAiCompatibleProvider implements AiProviderStrategy {

    private static final Set<String> SUPPORTED = Set.of("deepseek", "openai", "qwen", "kimi");

    /**
     * 内置默认系统提示词（模型未配置 systemPrompt 时兑底）
     * <p>排版规范浓缩自 AgentOne persona.md：emoji 分节 + Markdown 结构化，禁报告体。</p>
     */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是「AI wu助手」，为企业管理系统用户提供智能问答服务，回答准确、简洁、可执行。

            回答格式要求（必须遵守）：
            - 使用 Markdown 排版：加粗、列表、表格、引用块；标题 # 后必须有空格。
            - 小节用 emoji + 加粗标题（如 📌 **结论**、⚙️ **配置**、📋 **明细**、🎯 **步骤**、💡 **提示**、⚠️ **注意**），每条回答约 3~8 个 emoji，勿堆砌。
            - 并列信息、步骤、字段用列表或表格，超过 3 行的内容必须分节或列表化。
            - 简单问题 1~2 节直接作答，开门见山；复杂问题再多节展开。
            - 禁止《问题分析》《处理建议》等报告体标题，禁止大段无结构纯文字。
            - 链接统一 [说明文字](URL) 格式，不要裸贴长 URL。
            - 使用中文，避免「很高兴为您服务」等套话；不确定的内容诚实说明，不编造数据。
            """;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public boolean supports(String provider) {
        return provider != null && SUPPORTED.contains(provider.toLowerCase());
    }

    @Override
    public void streamChat(AiModelDO model, String plainApiKey,
                           List<AiChatStreamReqVO.ChatMessage> messages, AiStreamListener listener) throws Exception {
        JSONObject body = buildRequestBody(model, messages, true);
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

        int promptTokens = 0;
        int completionTokens = 0;
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
                    JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                    String content = delta == null ? null : delta.getStr("content");
                    if (StrUtil.isNotEmpty(content)) {
                        listener.onDelta(content);
                    }
                }
                JSONObject usage = chunk.getJSONObject("usage");
                if (usage != null) {
                    promptTokens = usage.getInt("prompt_tokens", 0);
                    completionTokens = usage.getInt("completion_tokens", 0);
                }
            }
        }
        listener.onComplete(promptTokens, completionTokens);
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

    private JSONObject buildRequestBody(AiModelDO model, List<AiChatStreamReqVO.ChatMessage> messages, boolean stream) {
        JSONObject body = new JSONObject();
        body.set("model", model.getModelName());
        body.set("stream", stream);
        if (model.getTemperature() != null) {
            body.set("temperature", model.getTemperature());
        }
        if (model.getMaxTokens() != null && model.getMaxTokens() > 0) {
            body.set("max_tokens", model.getMaxTokens());
        }
        if (stream) {
            // 请求供应商在最后一个 chunk 返回 usage 统计（OpenAI 兼容协议扩展，不支持的厂商会忽略）
            body.set("stream_options", new JSONObject().set("include_usage", true));
        }
        JSONArray msgArray = new JSONArray();
        String systemPrompt = StrUtil.blankToDefault(model.getSystemPrompt(), DEFAULT_SYSTEM_PROMPT);
        msgArray.add(new JSONObject().set("role", "system").set("content", systemPrompt));
        for (AiChatStreamReqVO.ChatMessage msg : messages) {
            msgArray.add(new JSONObject().set("role", msg.getRole()).set("content", msg.getContent()));
        }
        body.set("messages", msgArray);
        return body;
    }

    private String readErrorBody(InputStream body) {
        try (InputStream is = body) {
            return StrUtil.brief(new String(is.readAllBytes(), StandardCharsets.UTF_8), 300);
        } catch (Exception e) {
            return "";
        }
    }
}
