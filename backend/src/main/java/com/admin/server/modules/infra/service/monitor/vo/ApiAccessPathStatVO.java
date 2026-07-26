package com.admin.server.modules.infra.service.monitor.vo;

import lombok.Data;

@Data
public class ApiAccessPathStatVO {
    private String apiPath;
    private Long count;
}
