package com.admin.server.modules.infra.dal.dataobject.monitor;

import lombok.Data;

/**
 * API 访问方法统计聚合数据对象
 */
@Data
public class ApiAccessMethodStatDO {
    private String method;
    private Long count;
}
