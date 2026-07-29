package com.admin.server.modules.ai.util;

import cn.hutool.core.util.StrUtil;

/**
 * AI 知识库正文清洗与校验（L2 检索注入防护）
 * <p>
 * 设计方案 §4.2：管理端录入时校验 Markdown 围栏成对闭合；注入 Prompt 前仅对
 * 未闭合围栏做兼容处理，正常内容原样注入，避免破坏知识中合法代码示例的渲染。
 * </p>
 */
public final class AiKnowledgeSanitizer {

    private static final String FENCE = "```";

    private AiKnowledgeSanitizer() {
    }

    /** 校验正文中的三反引号围栏是否成对闭合（偶数个即闭合） */
    public static boolean isFenceBalanced(String content) {
        if (StrUtil.isBlank(content)) {
            return true;
        }
        int count = 0;
        int idx = content.indexOf(FENCE);
        while (idx >= 0) {
            count++;
            idx = content.indexOf(FENCE, idx + FENCE.length());
        }
        return count % 2 == 0;
    }

    /**
     * 注入 Prompt 前的兼容处理：仅当围栏未闭合时补齐结尾闭合，正常内容原样返回，
     * 不破坏知识中合法的代码块。
     */
    public static String sanitizeForPrompt(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        if (isFenceBalanced(content)) {
            return content;
        }
        return content + "\n" + FENCE;
    }
}
