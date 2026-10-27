package com.admin.server.modules.infra.dal.dataobject.monitor;

import lombok.Data;

/**
 * API 访问日志每日统计聚合数据对象
 */
@Data
public class ApiAccessDailyStatDO {
    /** yyyy-MM-dd */
    private String statDate;
    private Long total;
    private Long successCount;
}
