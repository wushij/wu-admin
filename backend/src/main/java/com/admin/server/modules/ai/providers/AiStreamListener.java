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
}
