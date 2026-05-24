package com.admin.server.modules.ai.service.impl;

import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KnowledgeRetriever 单元测试")
class KnowledgeRetrieverTest {

    @Mock
    private AiKnowledgeMapper aiKnowledgeMapper;

    @InjectMocks
    private KnowledgeRetriever retriever;

    private AiKnowledgeDO knowledge(long id, String title, String content) {
        AiKnowledgeDO k = new AiKnowledgeDO();
        k.setId(id);
        k.setTitle(title);
        k.setContent(content);
        k.setStatus(1);
        return k;
    }

    @Test
    @DisplayName("空查询返回空串，不触发检索")
    void blankQuery() {
        assertEquals("", retriever.retrieveReferenceBlock(""));
        assertEquals("", retriever.retrieveReferenceBlock(null));
        verify(aiKnowledgeMapper, never()).searchFullText(anyString(), anyInt());
    }

    @Test
    @DisplayName("全文命中：拼装 reference_data 块含标题与正文")
    void fullTextHit() {
        when(aiKnowledgeMapper.searchFullText(anyString(), anyInt()))
                .thenReturn(List.of(knowledge(1L, "忘记密码找回", "打开登录页点击忘记密码")));

        String block = retriever.retrieveReferenceBlock("密码找不回怎么办");

        assertTrue(block.contains("<reference_data>"));
        assertTrue(block.contains("</reference_data>"));
        assertTrue(block.contains("忘记密码找回"));
        assertTrue(block.contains("打开登录页点击忘记密码"));
    }

    @Test
    @DisplayName("全文异常降级 LIKE，仍能拼装结果")
    void fullTextErrorFallbackLike() {
        when(aiKnowledgeMapper.searchFullText(anyString(), anyInt()))
                .thenThrow(new RuntimeException("ngram not available"));
        when(aiKnowledgeMapper.selectList(any()))
                .thenReturn(List.of(knowledge(2L, "工单提交", "进入工单管理新建工单")));

        String block = retriever.retrieveReferenceBlock("怎么提交工单");

        assertTrue(block.contains("工单提交"));
        verify(aiKnowledgeMapper).selectList(any());
    }

    @Test
    @DisplayName("全文空 + LIKE 空：返回空串")
    void noHit() {
        when(aiKnowledgeMapper.searchFullText(anyString(), anyInt())).thenReturn(List.of());
        when(aiKnowledgeMapper.selectList(any())).thenReturn(List.of());

        assertEquals("", retriever.retrieveReferenceBlock("无关问题"));
    }
}
