package com.admin.server.modules.infra.dal.dataobject.monitor;

import lombok.Data;

/**
 * API 用户访问排行统计聚合数据对象
 */
@Data
public class ApiAccessUserStatDO {
    private Long userId;
    private Long count;
}
