package com.admin.server.modules.ai.tool.impl;

import cn.hutool.json.JSONObject;
import com.admin.server.modules.ai.tool.AiTool;
import com.admin.server.modules.ai.tool.AiToolContext;
import com.admin.server.modules.ai.tool.ToolAccessLevel;
import com.admin.server.modules.infra.service.monitor.OnlineUserService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 工具：查询当前在线用户数（ADMIN 级，需 monitor:online:list）
 */
@Component
public class GetOnlineUserCountTool implements AiTool {

    @Resource
    private OnlineUserService onlineUserService;

    @Override
    public String name() {
        return "get_online_user_count";
    }

    @Override
    public String description() {
        return "查询当前系统的在线用户数量。当用户询问「现在有多少人在线」「在线人数」等实时数据时调用。";
    }

    @Override
    public JSONObject parametersSchema() {
        return new JSONObject().set("type", "object").set("properties", new JSONObject());
    }

    @Override
    public ToolAccessLevel accessLevel() {
        return ToolAccessLevel.ADMIN;
    }

    @Override
    public String requiredPermission() {
        return "monitor:online:list";
    }

    @Override
    public String execute(JSONObject args, AiToolContext ctx) {
        int count = onlineUserService.countOnlineUsers();
        return new JSONObject().set("onlineUserCount", count).toString();
    }
}
