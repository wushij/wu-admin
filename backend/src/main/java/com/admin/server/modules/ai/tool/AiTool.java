package com.admin.server.modules.ai.tool;

import cn.hutool.json.JSONObject;

/**
 * AI 只读工具接口（L3 Function Calling）
 * <p>
 * 所有实现均为只读、聚合级操作，禁止行级敏感数据与任何写操作。
 * 工具以 Spring Bean 注册，由 AiToolExecutor 白名单管理与鉴权后调用。
 * </p>
 */
public interface AiTool {

    /** 工具名（OpenAI function.name，需全局唯一，蛇形命名） */
    String name();

    /** 工具用途描述（提供给模型判断是否调用） */
    String description();

    /**
     * OpenAI function parameters JSON Schema；无入参时返回
     * {"type":"object","properties":{}}。禁止声明身份类参数（userId 等）。
     */
    JSONObject parametersSchema();

    /** 访问级别（ADMIN 需权限校验，USER 登录即可） */
    ToolAccessLevel accessLevel();

    /**
     * ADMIN 级工具所需的权限标识（如 monitor:online:list）；USER 级返回 null。
     * 执行前二次鉴权与可见性过滤均依据此项。
     */
    String requiredPermission();

    /**
     * 执行工具，返回给模型的结果文本（建议 JSON 或简短自然语言）。
     *
     * @param args 模型传入的入参（已按 schema 解析，可能为空对象）
     * @param ctx  服务端注入的执行上下文（身份以此为准）
     */
    String execute(JSONObject args, AiToolContext ctx);
}
