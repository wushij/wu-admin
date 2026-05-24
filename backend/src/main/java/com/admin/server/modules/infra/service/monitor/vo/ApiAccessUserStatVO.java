package com.admin.server.modules.infra.service.monitor.vo;

import lombok.Data;

@Data
public class ApiAccessUserStatVO {
    private Long userId;
    private Long count;
}
