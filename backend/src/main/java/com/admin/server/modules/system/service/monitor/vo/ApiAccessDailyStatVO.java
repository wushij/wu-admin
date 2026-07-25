package com.admin.server.modules.system.service.monitor.vo;

import lombok.Data;

@Data
public class ApiAccessDailyStatVO {
    /** yyyy-MM-dd */
    private String statDate;
    private Long total;
    private Long successCount;
}
