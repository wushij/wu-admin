package cn.rbac.server.modules.system.service.monitor.vo;

import lombok.Data;

@Data
public class ApiAccessMethodStatVO {
    private String method;
    private Long count;
}
