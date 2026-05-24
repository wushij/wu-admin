package com.admin.server.modules.ai.tool.impl;

import cn.hutool.json.JSONObject;
import com.admin.server.modules.ai.tool.AiTool;
import com.admin.server.modules.ai.tool.AiToolContext;
import com.admin.server.modules.ai.tool.ToolAccessLevel;
import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 工具：查询近 N 天登录成功/失败次数（ADMIN 级，需 monitor:loginlog:list）
 */
@Component
public class GetLoginStatTool implements AiTool {

    @Resource
    private LoginLogMapper loginLogMapper;

    @Override
    public String name() {
        return "get_login_stat";
    }

    @Override
    public String description() {
        return "统计近 N 天的登录成功与失败次数。当用户询问登录统计、近期登录情况、登录失败次数等问题时调用。";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject days = new JSONObject()
                .set("type", "integer")
                .set("description", "统计的天数，默认 7，范围 1~30");
        return new JSONObject()
                .set("type", "object")
                .set("properties", new JSONObject().set("days", days));
    }

    @Override
    public ToolAccessLevel accessLevel() {
        return ToolAccessLevel.ADMIN;
    }

    @Override
    public String requiredPermission() {
        return "monitor:loginlog:list";
    }

    @Override
    public String execute(JSONObject args, AiToolContext ctx) {
        int days = args == null ? 7 : args.getInt("days", 7);
        if (days < 1) {
            days = 1;
        } else if (days > 30) {
            days = 30;
        }
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        // 登录日志状态：0=成功 1=失败
        Long success = loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLogDO>()
                .eq(LoginLogDO::getStatus, 0)
                .ge(LoginLogDO::getLoginTime, since));
        Long fail = loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLogDO>()
                .eq(LoginLogDO::getStatus, 1)
                .ge(LoginLogDO::getLoginTime, since));
        return new JSONObject()
                .set("days", days)
                .set("successCount", success == null ? 0 : success)
                .set("failCount", fail == null ? 0 : fail)
                .toString();
    }
}
