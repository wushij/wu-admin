package com.admin.server.modules.ai.service.impl;

import com.admin.server.modules.ai.api.vo.AiChatHistoryItemVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.admin.server.modules.ai.dal.mysql.AiChatLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatLogServiceImpl 历史会话回放单元测试")
class AiChatLogServiceImplTest {

    @Mock
    private AiChatLogMapper aiChatLogMapper;

    @InjectMocks
    private AiChatLogServiceImpl aiChatLogService;

    @Test
    @DisplayName("listHistory：正确反序列化联网参考来源 citationsJson，并在坏 JSON 时安全降级")
    void listHistory_shouldRestoreCitationsAndFallbackOnInvalidJson() {
        ReflectionTestUtils.setField(aiChatLogService, "baseMapper", aiChatLogMapper);

        AiChatLogDO validLog = new AiChatLogDO();
        validLog.setId(1L);
        validLog.setQuestion("今天佛山天气如何");
        validLog.setAnswer("佛山近期多云为主");
        validLog.setReasoning("用户询问佛山天气，先调用联网检索核实实时预报。");
        validLog.setThinkingDurationMs(4200L);
        validLog.setToolTrace("[{\"tool\":\"web_search\",\"denied\":false,\"durationMs\":1100}]");
        validLog.setChatStatus(1);
        validLog.setCitationsJson("[{\"title\":\"中国天气网\",\"url\":\"https://www.weather.com.cn\",\"snippet\":\"佛山天气预报\",\"source\":\"Tavily 检索\"}]");

        AiChatLogDO corruptLog = new AiChatLogDO();
        corruptLog.setId(2L);
        corruptLog.setQuestion("损坏测试");
        corruptLog.setAnswer("回答内容");
        corruptLog.setToolTrace("{broken-tool-trace");
        corruptLog.setChatStatus(1);
        corruptLog.setCitationsJson("{not-a-valid-json");

        AiChatLogDO failedLog = new AiChatLogDO();
        failedLog.setId(3L);
        failedLog.setQuestion("失败测试");
        failedLog.setAnswer("失败部分");
        failedLog.setReasoning("中断前思考");
        failedLog.setThinkingDurationMs(1200L);
        failedLog.setChatStatus(0);
        failedLog.setCitationsJson("[{\"title\":\"应丢弃\",\"url\":\"https://example.com\"}]");

        when(aiChatLogMapper.selectList(any())).thenReturn(List.of(validLog, corruptLog, failedLog));

        List<AiChatHistoryItemVO> history = aiChatLogService.listHistory(100L, "conv-1");
        assertEquals(3, history.size());

        assertEquals(1, history.get(0).getCitations().size());
        assertEquals("中国天气网", history.get(0).getCitations().get(0).getTitle());
        assertEquals("https://www.weather.com.cn", history.get(0).getCitations().get(0).getUrl());
        assertEquals("用户询问佛山天气，先调用联网检索核实实时预报。", history.get(0).getReasoning());
        assertEquals(4200L, history.get(0).getThinkingDurationMs());
        assertEquals(List.of("已完成联网检索"), history.get(0).getToolMessages());

        assertTrue(history.get(1).getCitations().isEmpty());
        assertTrue(history.get(1).getToolMessages().isEmpty());
        assertEquals("", history.get(2).getAnswer());
        assertEquals("", history.get(2).getReasoning());
        assertNull(history.get(2).getThinkingDurationMs());
        assertTrue(history.get(2).getCitations().isEmpty());
    }
}
