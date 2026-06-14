package cn.rbac.server.modules.system.service.monitor.vo;

import lombok.Data;

@Data
public class ApiAccessUserStatVO {
    private Long userId;
    private Long count;
}
