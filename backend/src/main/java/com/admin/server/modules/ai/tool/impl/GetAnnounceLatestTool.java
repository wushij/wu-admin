package com.admin.server.modules.ai.tool.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.admin.server.modules.ai.tool.AiTool;
import com.admin.server.modules.ai.tool.AiToolContext;
import com.admin.server.modules.ai.tool.ToolAccessLevel;
import com.admin.server.modules.message.dal.dataobject.AnnounceDO;
import com.admin.server.modules.message.dal.mysql.AnnounceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 工具：查询最新公告标题列表（USER 级，仅已发布公告）
 */
@Component
public class GetAnnounceLatestTool implements AiTool {

    private static final int MAX_LIMIT = 5;

    @Resource
    private AnnounceMapper announceMapper;

    @Override
    public String name() {
        return "get_announce_latest";
    }

    @Override
    public String description() {
        return "查询系统最新发布的公告标题列表。当用户询问「最新公告」「有什么通知」等问题时调用。";
    }

    @Override
    public JSONObject parametersSchema() {
        return new JSONObject().set("type", "object").set("properties", new JSONObject());
    }

    @Override
    public ToolAccessLevel accessLevel() {
        return ToolAccessLevel.USER;
    }

    @Override
    public String requiredPermission() {
        return null;
    }

    @Override
    public String execute(JSONObject args, AiToolContext ctx) {
        // status=1 表示已发布公告，仅取标题与时间，最多 5 条
        List<AnnounceDO> list = announceMapper.selectList(new LambdaQueryWrapper<AnnounceDO>()
                .eq(AnnounceDO::getStatus, 1)
                .orderByDesc(AnnounceDO::getCreateTime)
                .last("LIMIT " + MAX_LIMIT));
        JSONArray arr = new JSONArray();
        for (AnnounceDO a : list) {
            arr.add(new JSONObject()
                    .set("title", StrUtil.nullToEmpty(a.getTitle()))
                    .set("time", a.getCreateTime() == null ? "" : a.getCreateTime().toString()));
        }
        return new JSONObject().set("announcements", arr).toString();
    }
}
