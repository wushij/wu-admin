package com.admin.server.modules.ai.service.search;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.service.search.impl.AggregatedWebSearchService;
import com.admin.server.modules.ai.service.search.impl.CustomWebSearchExecutor;
import com.admin.server.modules.ai.service.search.impl.TavilyWebSearchService;
import com.admin.server.modules.ai.service.search.vo.CustomWebSearchEngineConfig;
import com.admin.server.modules.ai.service.search.vo.WebSearchCitationVO;
import com.admin.server.modules.ai.service.search.vo.WebSearchResult;
import com.admin.server.modules.system.api.config.SysConfigGroupController;
import com.admin.server.modules.system.dal.dataobject.config.SysConfigGroupDO;
import com.admin.server.modules.system.dal.mysql.config.SysConfigGroupMapper;
import com.admin.server.modules.system.framework.cache.SysConfigCacheService;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.config.impl.SysConfigGroupServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("联网搜索多引擎与第三方配置安全单元测试")
class WebSearchServiceTest {

    @Mock
    private SystemConfigHelper systemConfigHelper;
    @Mock
    private TavilyWebSearchService tavilyWebSearchService;
    @Mock
    private CustomWebSearchExecutor customWebSearchExecutor;
    @Mock
    private SysConfigGroupMapper configGroupMapper;
    @Mock
    private SysConfigCacheService sysConfigCacheService;

    @InjectMocks
    private AggregatedWebSearchService aggregatedWebSearchService;

    @InjectMocks
    private WebSearchContextBuilder webSearchContextBuilder;

    @InjectMocks
    private SysConfigGroupServiceImpl sysConfigGroupService;

    @Test
    @DisplayName("SSRF 防护：拦截回环、云元数据内网与非法协议，放行公网 HTTPS")
    void validatePublicEndpoint_shouldBlockInternalAndInvalidUrls() {
        assertThrows(BusinessException.class,
                () -> CustomWebSearchExecutor.validatePublicEndpoint("http://127.0.0.1:8080/search"));
        assertThrows(BusinessException.class,
                () -> CustomWebSearchExecutor.validatePublicEndpoint("http://169.254.169.254/latest/meta-data"));
        assertThrows(BusinessException.class,
                () -> CustomWebSearchExecutor.validatePublicEndpoint("ftp://api.deepseek.com/chat"));
        assertDoesNotThrow(() -> CustomWebSearchExecutor.validatePublicEndpoint("https://1.1.1.1/dns-query"));
    }

    @Test
    @DisplayName("自定义 HTTP JSON 点号路径解析：正确提取博查等嵌套列表字段")
    void customExecutor_shouldExtractNestedHttpJsonResults() {
        CustomWebSearchExecutor executor = new CustomWebSearchExecutor();
        CustomWebSearchEngineConfig engine = new CustomWebSearchEngineConfig();
        engine.setName("博查联网");
        engine.setItemsPath("data.webPages.value");
        engine.setTitleField("name");
        engine.setUrlField("url");
        engine.setSnippetField("summary");

        String rawJson = """
                {
                  "code": 200,
                  "data": {
                    "webPages": {
                      "value": [
                        {
                          "name": "Spring Boot 3.5 发布说明",
                          "url": "https://spring.io/blog/boot-3-5",
                          "summary": "提供多项云原生增强。"
                        },
                        {
                          "name": "非法链接应过滤",
                          "url": "javascript:alert(1)",
                          "summary": "xss"
                        }
                      ]
                    }
                  }
                }
                """;

        @SuppressWarnings("unchecked")
        List<WebSearchResult> results = (List<WebSearchResult>) ReflectionTestUtils.invokeMethod(
                executor, "parseHttpJsonResponse", rawJson, engine, 5);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Spring Boot 3.5 发布说明", results.get(0).getTitle());
        assertEquals("https://spring.io/blog/boot-3-5", results.get(0).getUrl());
        assertEquals("博查联网", results.get(0).getSource());
    }

    @Test
    @DisplayName("检索上下文构建：过滤非法/占位链接并生成参考溯源卡片")
    void contextBuilder_shouldFilterCitationsAndAppendPrompt() {
        WebSearchService mockSearchService = mock(WebSearchService.class);
        ReflectionTestUtils.setField(webSearchContextBuilder, "webSearchService", mockSearchService);
        when(mockSearchService.search("DeepSeek R1", 5)).thenReturn(List.of(
                WebSearchResult.builder()
                        .title("DeepSeek 官方发布")
                        .url("https://api-docs.deepseek.com/news")
                        .snippet("支持原生工具调用与推理")
                        .source("DeepSeek 联网搜索")
                        .build(),
                WebSearchResult.builder()
                        .title("Tavily AI 摘要")
                        .url("https://tavily.com")
                        .snippet("摘要文本")
                        .source("Tavily AI Answer")
                        .build(),
                WebSearchResult.builder()
                        .title("非 HTTP 协议")
                        .url("file:///etc/passwd")
                        .snippet("敏感内容")
                        .build()
        ));
        when(mockSearchService.formatSearchResultsForPrompt(any())).thenReturn("[参考来源 block]");

        List<WebSearchCitationVO> citations = new ArrayList<>();
        String prompt = webSearchContextBuilder.enrichWithWebSearch("DeepSeek R1", 5, "基础提示词", citations);

        assertTrue(prompt.contains("基础提示词"));
        assertTrue(prompt.contains("[参考来源 block]"));
        assertEquals(1, citations.size());
        assertEquals("https://api-docs.deepseek.com/news", citations.get(0).getUrl());
        assertEquals("DeepSeek 联网搜索", citations.get(0).getSource());
    }

    @Test
    @DisplayName("多引擎聚合：custom_first 优先命中自定义启用引擎，并正确输出生效摘要")
    void aggregatedService_shouldPreferCustomEngineWhenCustomFirst() {
        JSONObject thirdParty = JSONUtil.parseObj("""
                {
                  "tavily": {"enabled": true},
                  "webSearch": {
                    "enabled": true,
                    "strategy": "custom_first",
                    "fallbackToDuckDuckGo": false,
                    "customProviders": [
                      {"id": "ds-1", "name": "DeepSeek 联网", "enabled": true, "priority": 2, "endpointUrl": "https://api.deepseek.com/v1"},
                      {"id": "bc-1", "name": "博查搜索", "enabled": true, "priority": 1, "endpointUrl": "https://api.bochaai.com/v1/web-search"}
                    ]
                  }
                }
                """);
        when(systemConfigHelper.getGroupJson(SystemConfigHelper.GROUP_THIRD_PARTY)).thenReturn(thirdParty);
        when(tavilyWebSearchService.getEffectiveConfig()).thenReturn(
                new TavilyWebSearchService.RuntimeTavilyConfig(true, "tvly-test", "basic", "general", true, "https://api.tavily.com", 5));
        when(customWebSearchExecutor.execute(argThat(c -> "bc-1".equals(c.getId())), eq("今日要闻"), eq(5)))
                .thenReturn(List.of(WebSearchResult.builder()
                        .title("今日要闻")
                        .url("https://news.example.com/1")
                        .snippet("摘要")
                        .source("博查搜索")
                        .build()));

        assertTrue(aggregatedWebSearchService.isCapabilityEnabled());
        String summary = aggregatedWebSearchService.resolveActiveSummary();
        assertTrue(summary.contains("博查搜索"));
        assertTrue(summary.contains("DeepSeek 联网"));
        assertTrue(summary.contains("Tavily"));

        List<WebSearchResult> results = aggregatedWebSearchService.search("今日要闻", 5);
        assertEquals(1, results.size());
        assertEquals("博查搜索", results.get(0).getSource());
        verify(tavilyWebSearchService, never()).search(anyString(), anyInt());
    }

    @Test
    @DisplayName("配置校验与密钥脱敏：拦截非 JSON 扩展体与内网地址，占位符提交不覆盖原密钥")
    void sysConfigGroup_shouldMaskCustomProviderKeysAndPreserveSecrets() {
        SysConfigGroupController controller = new SysConfigGroupController();
        ReflectionTestUtils.setField(controller, "configGroupService", sysConfigGroupService);
        ReflectionTestUtils.setField(controller, "systemConfigHelper", systemConfigHelper);

        JSONObject oldJson = JSONUtil.parseObj("""
                {
                  "tavily": {"enabled": true, "apiKey": "tvly-real-secret-key"},
                  "webSearch": {
                    "enabled": true,
                    "customProviders": [
                      {"id": "ds-1", "name": "DeepSeek", "apiKey": "sk-real-ds-key", "endpointUrl": "https://api.deepseek.com/v1"}
                    ]
                  }
                }
                """);
        when(systemConfigHelper.getGroupJson(SystemConfigHelper.GROUP_THIRD_PARTY)).thenReturn(oldJson);

        // 1. 读取时必须统一脱敏成占位符
        SysConfigGroupDO stored = new SysConfigGroupDO();
        stored.setId(1L);
        stored.setGroupCode(SystemConfigHelper.GROUP_THIRD_PARTY);
        stored.setConfigValue(oldJson.toString());
        when(configGroupMapper.selectOne(any())).thenReturn(stored);

        SysConfigGroupDO maskedDo = controller.get(SystemConfigHelper.GROUP_THIRD_PARTY).getData();
        JSONObject maskedJson = JSONUtil.parseObj(maskedDo.getConfigValue());
        assertNotEquals("tvly-real-secret-key", maskedJson.getByPath("tavily.apiKey"));
        assertTrue(String.valueOf(maskedJson.getByPath("tavily.apiKey")).contains("•"));
        JSONArray maskedProviders = maskedJson.getJSONObject("webSearch").getJSONArray("customProviders");
        assertTrue(maskedProviders.getJSONObject(0).getStr("apiKey").contains("•"));

        // 2. 提交占位符/空值更新时保留原真实密钥
        String updateBody = """
                {
                  "tavily": {"enabled": true, "apiKey": "••••••••••••••••••••••••••••", "maxResults": 5},
                  "webSearch": {
                    "enabled": true,
                    "customProviders": [
                      {"id": "ds-1", "name": "DeepSeek", "apiKey": "", "endpointUrl": "https://1.1.1.1/v1", "timeoutMs": 15000}
                    ]
                  }
                }
                """;
        controller.update(SystemConfigHelper.GROUP_THIRD_PARTY, Map.of("configValue", updateBody));
        verify(configGroupMapper).updateById(argThat((SysConfigGroupDO entity) -> {
            JSONObject saved = JSONUtil.parseObj(entity.getConfigValue());
            String savedTavilyKey = saved.getJSONObject("tavily").getStr("apiKey");
            String savedCustomKey = saved.getJSONObject("webSearch").getJSONArray("customProviders")
                    .getJSONObject(0).getStr("apiKey");
            return "tvly-real-secret-key".equals(savedTavilyKey) && "sk-real-ds-key".equals(savedCustomKey);
        }));

        // 3. 校验拦截非法 extraBodyJson 与内网地址
        String invalidJsonExtra = """
                {
                  "webSearch": {
                    "customProviders": [
                      {"id": "ds-2", "name": "BadExtra", "enabled": true, "endpointUrl": "https://1.1.1.1/v1", "extraBodyJson": "{not-json}"}
                    ]
                  }
                }
                """;
        assertThrows(BusinessException.class,
                () -> sysConfigGroupService.updateConfig(SystemConfigHelper.GROUP_THIRD_PARTY, invalidJsonExtra));

        String ssrfBody = """
                {
                  "webSearch": {
                    "customProviders": [
                      {"id": "ds-3", "name": "LocalSSRF", "enabled": true, "endpointUrl": "http://127.0.0.1:9000/v1"}
                    ]
                  }
                }
                """;
        assertThrows(BusinessException.class,
                () -> sysConfigGroupService.updateConfig(SystemConfigHelper.GROUP_THIRD_PARTY, ssrfBody));

        // 4. 代理 Fake-IP 环境下域名不做本地 DNS 误判，DeepSeek 公网域名应允许保存；本机 localhost 仍须拦截
        String deepseekBody = """
                {
                  "webSearch": {
                    "enabled": true,
                    "strategy": "custom_first",
                    "customProviders": [
                      {"id": "ds-ok", "name": "DeepSeek 联网搜索", "enabled": true, "endpointUrl": "https://api.deepseek.com/v1/tools/search", "timeoutMs": 15000}
                    ]
                  }
                }
                """;
        assertDoesNotThrow(() -> sysConfigGroupService.updateConfig(SystemConfigHelper.GROUP_THIRD_PARTY, deepseekBody));

        String localhostBody = """
                {
                  "webSearch": {
                    "customProviders": [
                      {"id": "ds-local", "name": "LocalhostSSRF", "enabled": true, "endpointUrl": "http://localhost:9000/v1"}
                    ]
                  }
                }
                """;
        BusinessException localEx = assertThrows(BusinessException.class,
                () -> sysConfigGroupService.updateConfig(SystemConfigHelper.GROUP_THIRD_PARTY, localhostBody));
        assertFalse(localEx.getMessage().contains("告警 webhook"));
    }

    @Test
    @DisplayName("联网搜索关键词清洗、time_range 推导与反爬拦截页过滤")
    void queryNormalizationAndNoiseFiltering_shouldPreventTicketAndCaptchaNoise() {
        assertEquals("佛山天气", TavilyWebSearchService.normalizeSearchQuery("请问佛山天气怎么样？"));
        assertEquals("今天佛山天气", TavilyWebSearchService.normalizeSearchQuery("帮我查一下今天佛山天气怎么样？"));
        assertFalse(TavilyWebSearchService.normalizeSearchQuery("今天佛山天气怎么样").contains("2026-11-24"));

        // 天气与今日咨询使用 week 窗口，突发新闻才使用 day，常规问题不限
        assertEquals("week", TavilyWebSearchService.resolveTimeRange("今天佛山天气"));
        assertEquals("day", TavilyWebSearchService.resolveTimeRange("刚刚发布的突发新闻"));
        assertEquals("week", TavilyWebSearchService.resolveTimeRange("最近 Spring Boot 发布版本"));
        assertNull(TavilyWebSearchService.resolveTimeRange("Vue 3 响应式原理"));

        // 拦截安全验证/反爬页，保留正常网页
        assertFalse(TavilyWebSearchService.isSafeWebResult("https://weather.example.com", "安全验证中...", "正在验证您的浏览器环境，请稍候..."));
        assertFalse(TavilyWebSearchService.isSafeWebResult("https://cf.example.com", "Just a moment...", "Checking your browser"));
        assertTrue(TavilyWebSearchService.isSafeWebResult("https://www.weather.com.cn", "佛山天气预报", "今日多云转晴，20~26℃"));

        WebSearchResult item = new WebSearchResult();
        item.setTitle("示例页面");
        item.setUrl("https://example.com");
        item.setSource("Tavily");
        item.setSnippet("摘要");
        String promptBlock = aggregatedWebSearchService.formatSearchResultsForPrompt(List.of(item));
        assertTrue(promptBlock.contains("本次联网检索执行时间："));
        assertTrue(promptBlock.contains("<current_time>"));
    }
}
