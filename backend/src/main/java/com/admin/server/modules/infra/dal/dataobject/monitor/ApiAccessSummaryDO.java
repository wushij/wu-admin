package com.admin.server.modules.infra.dal.dataobject.monitor;

import lombok.Data;

/**
 * API 访问汇总统计聚合数据对象
 */
@Data
public class ApiAccessSummaryDO {
    private Long totalCount;
    private Long successCount;
}
