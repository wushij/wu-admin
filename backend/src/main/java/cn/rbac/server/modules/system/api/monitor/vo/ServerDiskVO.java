package cn.rbac.server.modules.system.api.monitor.vo;

import lombok.Data;

@Data
public class ServerDiskVO {

    private String path;
    private String total;
    private String free;
    private String used;
    private Double usedPercent;
}
