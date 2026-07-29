package com.admin.server.modules.ai.providers;

/**
 * 大模型流式输出监听器
 */
public interface AiStreamListener {

    /** 收到一段增量文本 */
    void onDelta(String delta);

    /** 流结束，回传 Token 统计（供应商未返回时为 0） */
    void onComplete(int promptTokens, int completionTokens);

    /** 客户端是否已断开/中断（Provider 轮询此标记提前退出并断开与厂商的连接） */
    boolean isCancelled();

    // ---------- L3 Function Calling 扩展（默认关闭，旧实现无需改动） ----------

    /** 是否启用工具调用 */
    default boolean toolsEnabled() {
        return false;
    }

    /** OpenAI tools 声明数组 JSON（可见性过滤后）；无工具返回 null */
    default String toolSpecsJson() {
        return null;
    }

    /**
     * 执行一次工具调用并返回结果文本（鉴权/超时由实现方保证，绝不抛出）
     *
     * @param toolName 工具名
     * @param argsJson 模型给出的入参 JSON
     */
    default String executeTool(String toolName, String argsJson) {
        return null;
    }

    /** 工具执行前的状态通知（供 SSE 向前端推送“正在查询…”） */
    default void onToolStatus(String toolName) {
    }
}
