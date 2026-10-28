package com.admin.server.modules.ai.providers;

import com.admin.server.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("模型列表拉取工具单元测试")
class ProviderHttpUtilsTest {

    @Test
    @DisplayName("OpenAI 标准响应：解析 data[].id 并去重")
    void parsesOpenAiResponse() {
        assertEquals(List.of("m-1", "m-2"), ProviderHttpUtils.parseModelIds(
                "{\"object\":\"list\",\"data\":[{\"id\":\"m-1\"},{\"id\":\"m-2\"},{\"id\":\"m-1\"}]}"));
    }

    @Test
    @DisplayName("Ollama 响应：回退到 models[].name")
    void parsesOllamaResponse() {
        assertEquals(List.of("qwen3:8b", "llama3:latest"), ProviderHttpUtils.parseModelIds(
                "{\"models\":[{\"name\":\"qwen3:8b\"},{\"name\":\"llama3:latest\"}]}"));
    }

    @Test
    @DisplayName("网关直接返回数组或纯字符串数组")
    void parsesBareArray() {
        assertEquals(List.of("gpt-4o", "o3-mini"), ProviderHttpUtils.parseModelIds(
                "[{\"id\":\"gpt-4o\"},{\"id\":\"o3-mini\"}]"));
        assertEquals(List.of("deepseek-chat"), ProviderHttpUtils.parseModelIds("[\"deepseek-chat\"]"));
    }

    @Test
    @DisplayName("空响应返回空列表，非法 JSON 抛出业务异常")
    void handlesBlankAndInvalidBody() {
        assertEquals(List.of(), ProviderHttpUtils.parseModelIds(""));
        assertThrows(BusinessException.class, () -> ProviderHttpUtils.parseModelIds("<html>502</html>"));
    }

    @Test
    @DisplayName("地址解析：baseUrl 带 /v1 时只尝试一个地址")
    void resolvesV1BaseUrl() {
        assertEquals(List.of("https://api.deepseek.com/v1/models"),
                ProviderHttpUtils.resolveModelsUrls("https://api.deepseek.com/v1", "openai"));
    }

    @Test
    @DisplayName("地址解析：裸域名优先 /models，再兜底 /v1/models")
    void resolvesBareHost() {
        assertEquals(List.of("https://api.openai.com/models", "https://api.openai.com/v1/models"),
                ProviderHttpUtils.resolveModelsUrls("https://api.openai.com", "openai"));
    }

    @Test
    @DisplayName("地址解析：误填对话端点时回退到父路径")
    void resolvesChatEndpoint() {
        assertEquals(List.of("https://api.example.com/v1/models"),
                ProviderHttpUtils.resolveModelsUrls("https://api.example.com/v1/chat/completions", "openai"));
    }

    @Test
    @DisplayName("地址解析：Anthropic 协议直接走 /v1/models")
    void resolvesAnthropicBaseUrl() {
        assertEquals(List.of("https://api.anthropic.com/v1/models"),
                ProviderHttpUtils.resolveModelsUrls("https://api.anthropic.com", "anthropic"));
    }
}
