package com.admin.server.modules.ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AiKnowledgeSanitizer 单元测试")
class AiKnowledgeSanitizerTest {

    @Test
    @DisplayName("isFenceBalanced：成对/空/无围栏均视为闭合")
    void balanced() {
        assertTrue(AiKnowledgeSanitizer.isFenceBalanced(null));
        assertTrue(AiKnowledgeSanitizer.isFenceBalanced(""));
        assertTrue(AiKnowledgeSanitizer.isFenceBalanced("普通文本无围栏"));
        assertTrue(AiKnowledgeSanitizer.isFenceBalanced("前置\n```java\ncode\n```\n结尾"));
    }

    @Test
    @DisplayName("isFenceBalanced：奇数个围栏视为未闭合")
    void unbalanced() {
        assertFalse(AiKnowledgeSanitizer.isFenceBalanced("```java\ncode 未闭合"));
        assertFalse(AiKnowledgeSanitizer.isFenceBalanced("a```b```c```d"));
    }

    @Test
    @DisplayName("sanitizeForPrompt：闭合内容原样返回，未闭合补齐结尾围栏")
    void sanitize() {
        String balanced = "```\ncode\n```";
        assertEquals(balanced, AiKnowledgeSanitizer.sanitizeForPrompt(balanced));

        String result = AiKnowledgeSanitizer.sanitizeForPrompt("```\ncode 未闭合");
        assertTrue(AiKnowledgeSanitizer.isFenceBalanced(result), "补齐后应成对闭合");
        assertTrue(result.endsWith("```"));
    }
}
