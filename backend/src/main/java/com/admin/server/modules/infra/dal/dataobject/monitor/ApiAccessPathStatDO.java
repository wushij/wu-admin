package com.admin.server.modules.infra.dal.dataobject.monitor;

import lombok.Data;

/**
 * API 访问路径统计聚合数据对象
 */
@Data
public class ApiAccessPathStatDO {
    private String apiPath;
    private Long count;
}
