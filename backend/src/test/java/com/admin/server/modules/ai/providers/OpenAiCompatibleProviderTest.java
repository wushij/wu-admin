package com.admin.server.modules.ai.providers;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OpenAI 兼容协议 Provider 单元测试")
class OpenAiCompatibleProviderTest {

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> authHeader = new AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("正常 SSE：拼接文本增量并回传 usage")
    void parsesNormalStream() throws Exception {
        start(exchange -> sse(exchange,
                "{\"choices\":[{\"delta\":{\"content\":\"你\"}}]}",
                "{\"choices\":[{\"delta\":{\"content\":\"好\"}}]}",
                "{\"choices\":[],\"usage\":{\"prompt_tokens\":3,\"completion_tokens\":2}}",
                "[DONE]"));

        CollectingListener listener = new CollectingListener();
        new OpenAiCompatibleProvider().streamChat(model(), "sk-test", List.of(user("hi")), listener);

        assertEquals("你好", listener.text.toString());
        assertEquals(3, listener.promptTokens);
        assertEquals(2, listener.completionTokens);
        assertEquals("Bearer sk-test", authHeader.get());
    }

    @Test
    @DisplayName("上游拒绝 tools 时自动去掉扩展参数重试")
    void dropsToolsWhenUpstreamRejects() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        start(exchange -> {
            if (calls.incrementAndGet() == 1) {
                error(exchange, 400, "{\"error\":{\"message\":\"tools are not supported\"}}");
            } else {
                sse(exchange, "{\"choices\":[{\"delta\":{\"content\":\"ok\"}}]}", "[DONE]");
            }
        });

        CollectingListener listener = new CollectingListener();
        listener.enableTools = true;
        listener.toolSpecs = "[{\"type\":\"function\",\"function\":{\"name\":\"get_x\","
                + "\"description\":\"d\",\"parameters\":{\"type\":\"object\",\"properties\":{}}}}]";
        new OpenAiCompatibleProvider().streamChat(model(), "sk-test", List.of(user("hi")), listener);

        assertEquals("ok", listener.text.toString());
        assertFalse(lastBody.get().contains("\"tools\""));
        assertFalse(lastBody.get().contains("stream_options"));
    }

    @Test
    @DisplayName("上游提示 max_completion_tokens 时自动替换字段")
    void switchesToMaxCompletionTokens() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        start(exchange -> {
            if (calls.incrementAndGet() == 1) {
                error(exchange, 400, "{\"error\":{\"message\":\"Unsupported parameter: 'max_tokens' "
                        + "is not supported with this model. Use 'max_completion_tokens' instead.\"}}");
            } else {
                sse(exchange, "{\"choices\":[{\"delta\":{\"content\":\"ok\"}}]}", "[DONE]");
            }
        });

        new OpenAiCompatibleProvider().streamChat(model(), "sk-test", List.of(user("hi")), new CollectingListener());

        assertTrue(lastBody.get().contains("max_completion_tokens"));
        assertFalse(lastBody.get().contains("\"max_tokens\""));
    }

    @Test
    @DisplayName("SSE 内的 error 事件必须抛出，不能静默返回空回答")
    void throwsOnSseError() throws Exception {
        start(exchange -> sse(exchange,
                "{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"bad request\"}}"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> new OpenAiCompatibleProvider().streamChat(model(), "sk-test", List.of(user("hi")), new CollectingListener()));
        assertTrue(error.getMessage().contains("bad request"));
    }

    @Test
    @DisplayName("获取模型列表：请求 /models 并解析模型标识")
    void listsModels() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/models", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            json(exchange, "{\"data\":[{\"id\":\"m-1\"},{\"id\":\"m-2\"},{\"id\":\"m-1\"}]}");
        });
        server.start();

        List<String> models = new OpenAiCompatibleProvider().listModels(model(), "sk-test");

        assertEquals(List.of("m-1", "m-2"), models);
        assertEquals("Bearer sk-test", authHeader.get());
    }

    private void start(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            handler.handle(exchange);
        });
        server.start();
    }

    private AiModelDO model() {
        AiModelDO model = new AiModelDO();
        model.setModelName("test-model");
        model.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        model.setMaxTokens(128);
        model.setTemperature(0.7D);
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

    private static void error(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void json(HttpExchange exchange, String body) throws IOException {
        error(exchange, 200, body);
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
            return "tool-result";
        }
    }
}
