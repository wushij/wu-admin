package com.admin.server.modules.ai.tool.impl;

import cn.hutool.json.JSONObject;
import com.admin.server.modules.ai.tool.AiTool;
import com.admin.server.modules.ai.tool.AiToolContext;
import com.admin.server.modules.ai.tool.ToolAccessLevel;
import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalFormDO;
import com.admin.server.modules.ticket.dal.dataobject.ticket.TicketDO;
import com.admin.server.modules.ticket.dal.mysql.approval.ApprovalFormMapper;
import com.admin.server.modules.ticket.dal.mysql.ticket.TicketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 工具：查询「我的待办」数量（USER 级，仅限本人）
 * <p>统计待我审批的审批单（SUBMITTED）与指派给我的未完结工单（OPEN/IN_PROGRESS）。</p>
 */
@Component
public class GetMyTodoCountTool implements AiTool {

    @Resource
    private ApprovalFormMapper approvalFormMapper;

    @Resource
    private TicketMapper ticketMapper;

    @Override
    public String name() {
        return "get_my_todo_count";
    }

    @Override
    public String description() {
        return "查询当前登录用户的待办数量，包括待我审批的审批单与指派给我的未完结工单。当用户询问「我有多少待办」「我要审批什么」等问题时调用。";
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
        // 身份强制取自会话上下文，忽略模型传入的任何用户标识，杜绝横向越权
        Long userId = ctx.getUserId();
        Long pendingApproval = approvalFormMapper.selectCount(new LambdaQueryWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getApproverUserId, userId)
                .eq(ApprovalFormDO::getStatus, "SUBMITTED"));
        Long pendingTicket = ticketMapper.selectCount(new LambdaQueryWrapper<TicketDO>()
                .eq(TicketDO::getAssigneeUserId, userId)
                .in(TicketDO::getStatus, List.of("OPEN", "IN_PROGRESS")));
        long approval = pendingApproval == null ? 0 : pendingApproval;
        long ticket = pendingTicket == null ? 0 : pendingTicket;
        return new JSONObject()
                .set("pendingApprovalCount", approval)
                .set("pendingTicketCount", ticket)
                .set("totalTodoCount", approval + ticket)
                .toString();
    }
}
