package com.admin.server.modules.ai.providers;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Anthropic Messages 协议 Provider 单元测试")
class AnthropicProviderTest {

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> apiKeyHeader = new AtomicReference<>();
    private final AtomicReference<String> versionHeader = new AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("正常 SSE：text_delta 拼接、usage 与鉴权头正确")
    void parsesNormalStream() throws Exception {
        start(exchange -> sse(exchange,
                "{\"type\":\"message_start\",\"message\":{\"usage\":{\"input_tokens\":7,\"output_tokens\":0}}}",
                "{\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}",
                "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"你\"}}",
                "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"好\"}}",
                "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"},\"usage\":{\"output_tokens\":4}}",
                "{\"type\":\"message_stop\"}"));

        CollectingListener listener = new CollectingListener();
        new AnthropicProvider().streamChat(model(), "sk-ant-test", List.of(user("hi")), listener);

        assertEquals("你好", listener.text.toString());
        assertEquals(7, listener.promptTokens);
        assertEquals(4, listener.completionTokens);
        assertEquals("sk-ant-test", apiKeyHeader.get());
        assertEquals("2023-06-01", versionHeader.get());
        // system 由 Provider 提升到顶层，messages 中不再包含 system 角色
        JSONObject body = JSONUtil.parseObj(lastBody.get());
        assertTrue(body.containsKey("system"));
        assertEquals("user", body.getJSONArray("messages").getJSONObject(0).getStr("role"));
    }

    @Test
    @DisplayName("工具调用：tool_use 解析并回填 tool_result")
    void handlesToolUseRound() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        start(exchange -> {
            if (calls.incrementAndGet() == 1) {
                sse(exchange,
                        "{\"type\":\"message_start\",\"message\":{\"usage\":{\"input_tokens\":5,\"output_tokens\":0}}}",
                        "{\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}",
                        "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"查询中\"}}",
                        "{\"type\":\"content_block_start\",\"index\":1,\"content_block\":{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"get_x\"}}",
                        "{\"type\":\"content_block_delta\",\"index\":1,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"a\\\":1}\"}}",
                        "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"tool_use\"},\"usage\":{\"output_tokens\":6}}",
                        "{\"type\":\"message_stop\"}");
            } else {
                sse(exchange,
                        "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"完成\"}}",
                        "{\"type\":\"message_delta\",\"usage\":{\"output_tokens\":2}}",
                        "{\"type\":\"message_stop\"}");
            }
        });

        CollectingListener listener = new CollectingListener();
        listener.enableTools = true;
        listener.toolSpecs = "[{\"type\":\"function\",\"function\":{\"name\":\"get_x\","
                + "\"description\":\"d\",\"parameters\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"number\"}}}}}]";
        new AnthropicProvider().streamChat(model(), "sk-ant-test", List.of(user("hi")), listener);

        assertEquals("查询中完成", listener.text.toString());
        assertEquals("tool-result", listener.lastToolResult);
        JSONObject secondBody = JSONUtil.parseObj(lastBody.get());
        JSONArray tools = secondBody.getJSONArray("tools");
        assertTrue(tools.getJSONObject(0).containsKey("input_schema"));

        JSONArray messages = secondBody.getJSONArray("messages");
        JSONObject assistant = messages.getJSONObject(messages.size() - 2);
        JSONObject toolResult = messages.getJSONObject(messages.size() - 1);
        assertEquals("assistant", assistant.getStr("role"));
        // 第一轮有文本输出时，content 依次是 text、tool_use
        JSONArray assistantContent = assistant.getJSONArray("content");
        assertEquals("text", assistantContent.getJSONObject(0).getStr("type"));
        assertEquals("tool_use", assistantContent.getJSONObject(1).getStr("type"));
        assertEquals("user", toolResult.getStr("role"));
        assertEquals("tool_result", toolResult.getJSONArray("content").getJSONObject(0).getStr("type"));
        assertEquals("toolu_1", toolResult.getJSONArray("content").getJSONObject(0).getStr("tool_use_id"));
    }

    @Test
    @DisplayName("SSE 内的 error 事件必须抛出")
    void throwsOnSseError() throws Exception {
        start(exchange -> sse(exchange,
                "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"overloaded\"}}"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> new AnthropicProvider().streamChat(model(), "sk-ant-test", List.of(user("hi")), new CollectingListener()));
        assertTrue(error.getMessage().contains("overloaded"));
    }

    private void start(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiKeyHeader.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            versionHeader.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
            handler.handle(exchange);
        });
        server.start();
    }

    private AiModelDO model() {
        AiModelDO model = new AiModelDO();
        model.setModelName("claude-test");
        model.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        model.setMaxTokens(256);
        model.setTemperature(0.5D);
        return model;
    }

    private static AiChatStreamReqVO.ChatMessage user(String content) {
        AiChatStreamReqVO.ChatMessage message = new AiChatStreamReqVO.ChatMessage();
        message.setRole("user");
        message.setContent(content);
        return message;
    }

    private static void sse(HttpExchange exchange, String... payloads) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream os = exchange.getResponseBody()) {
            for (String payload : payloads) {
                os.write(("data: " + payload + "\n\n").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private static class CollectingListener implements AiStreamListener {
        final StringBuilder text = new StringBuilder();
        int promptTokens;
        int completionTokens;
        boolean enableTools;
        String toolSpecs;
        String lastToolResult;

        @Override
        public void onDelta(String delta) {
            text.append(delta);
        }

        @Override
        public void onComplete(int promptTokens, int completionTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean toolsEnabled() {
            return enableTools;
        }

        @Override
        public String toolSpecsJson() {
            return toolSpecs;
        }

        @Override
        public String executeTool(String toolName, String argsJson) {
            lastToolResult = "tool-result";
            return lastToolResult;
        }
    }
}
