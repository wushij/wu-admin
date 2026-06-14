package cn.rbac.server.modules.system.service.monitor.vo;

import lombok.Data;

@Data
public class ApiAccessPathStatVO {
    private String apiPath;
    private Long count;
}
