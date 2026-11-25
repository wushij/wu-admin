package com.admin.server.modules.ai.service.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownChunkSplitterTest {

    @Test
    void shouldSplitBySecondLevelHeading() {
        MarkdownChunkSplitter splitter = new MarkdownChunkSplitter(800, 128);
        String text = """
                ## 登录说明
                用户可以使用用户名和密码登录系统。

                ## 密码找回
                在登录页点击忘记密码，通过邮箱验证码重置。
                """;
        List<MarkdownChunkSplitter.Chunk> chunks = splitter.split(text);
        assertEquals(2, chunks.size());
        assertEquals("登录说明", chunks.get(0).heading());
        assertEquals("密码找回", chunks.get(1).heading());
        assertTrue(chunks.get(0).content().contains("用户名和密码"));
    }

    @Test
    void shouldRespectChunkSizeForLongSection() {
        MarkdownChunkSplitter splitter = new MarkdownChunkSplitter(300, 60);
        StringBuilder longText = new StringBuilder("## 长章节\n");
        for (int i = 0; i < 60; i++) {
            longText.append("这是第").append(i).append("段内容，用于验证超长章节会按目标字符数切片。\n\n");
        }
        List<MarkdownChunkSplitter.Chunk> chunks = splitter.split(longText.toString());
        assertTrue(chunks.size() > 1, "超长章节应被切成多片");
        for (MarkdownChunkSplitter.Chunk chunk : chunks) {
            // 允许在断点处略微超出，但不应无限膨胀
            assertTrue(chunk.content().length() <= 600,
                    "切片长度异常: " + chunk.content().length());
            assertFalse(chunk.content().isBlank());
        }
    }

    @Test
    void shouldNotBreakInsideFencedCodeBlock() {
        MarkdownChunkSplitter splitter = new MarkdownChunkSplitter(300, 40);
        StringBuilder text = new StringBuilder("## 代码示例\n");
        for (int i = 0; i < 30; i++) {
            text.append("说明文字").append(i).append("。\n");
        }
        text.append("```java\n");
        text.append("public class Demo {\n");
        for (int i = 0; i < 40; i++) {
            text.append("    // 代码行 ").append(i).append("\n");
        }
        text.append("}\n```\n");
        List<MarkdownChunkSplitter.Chunk> chunks = splitter.split(text.toString());
        for (MarkdownChunkSplitter.Chunk chunk : chunks) {
            long fences = chunk.content().lines().filter(line -> line.trim().startsWith("```")).count();
            assertEquals(0, fences % 2, "切片内代码围栏应成对出现，避免代码块被截断");
        }
    }

    @Test
    void shouldMergeHeadingOnlySection() {
        MarkdownChunkSplitter splitter = new MarkdownChunkSplitter(800, 128);
        String text = """
                ## 只有标题

                ## 有正文的章节
                这里是真正的内容，包含足够的文字用于形成有效切片。
                """;
        List<MarkdownChunkSplitter.Chunk> chunks = splitter.split(text);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.content().contains("真正的内容")));
    }

    @Test
    void shouldReturnEmptyForBlankText() {
        MarkdownChunkSplitter splitter = new MarkdownChunkSplitter(800, 128);
        assertTrue(splitter.split(null).isEmpty());
        assertTrue(splitter.split("   \n\n  ").isEmpty());
    }

    @Test
    void shouldEstimateChineseTokensHigherThanCharsDividedByFour() {
        int tokens = MarkdownChunkSplitter.estimateTokens("这是一段中文内容用于估算");
        assertTrue(tokens >= 12, "中文 token 估算不应明显偏低: " + tokens);
    }
}
