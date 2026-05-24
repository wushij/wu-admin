package com.admin.server.modules.system.service.dashboard.vo;

import lombok.Data;

/**
 * 工作台统计聚合查询单行结果（{@link com.admin.server.modules.system.dal.mysql.dashboard.DashboardMapper#selectAggregateStats}）
 */
@Data
public class DashboardStatsRow {

    private Long userCount;
    private Long userPendingCount;
    private Long userDisabledCount;
    private Long userToday;
    private Long userYesterday;

    private Long roleCount;
    private Long roleToday;
    private Long roleYesterday;

    private Long menuCount;

    private Long deptCount;
    private Long deptToday;
    private Long deptYesterday;

    private Long postCount;

    private Long todayLoginSuccess;
    private Long todayLoginFail;
    private Long yesterdayLoginSuccess;

    private Long ticketOpenCount;
    private Long ticketOverdueCount;

    private Long approvalPendingCount;

    private Long jobTotalCount;
    private Long jobRunningCount;
}
