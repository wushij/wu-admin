package com.admin.server.modules.infra.service.monitor.vo;

import lombok.Data;

@Data
public class ApiAccessMethodStatVO {
    private String method;
    private Long count;
}
