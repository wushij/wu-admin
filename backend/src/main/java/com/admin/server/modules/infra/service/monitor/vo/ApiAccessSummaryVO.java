package com.admin.server.modules.infra.service.monitor.vo;

import lombok.Data;

@Data
public class ApiAccessSummaryVO {
    private Long totalCount;
    private Long successCount;
}
