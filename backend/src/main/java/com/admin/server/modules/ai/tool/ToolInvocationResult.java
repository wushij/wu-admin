package com.admin.server.modules.ai.tool;

/**
 * 工具调用结果（含审计所需的元信息）
 */
public class ToolInvocationResult {

    private final String toolName;
    /** 回传给模型的结果文本（JSON 或话术） */
    private final String result;
    /** 是否被拒绝（无权限/未注册/超时/异常） */
    private final boolean denied;
    private final long durationMs;

    public ToolInvocationResult(String toolName, String result, boolean denied, long durationMs) {
        this.toolName = toolName;
        this.result = result;
        this.denied = denied;
        this.durationMs = durationMs;
    }

    public String getToolName() {
        return toolName;
    }

    public String getResult() {
        return result;
    }

    public boolean isDenied() {
        return denied;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
