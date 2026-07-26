package com.admin.server.modules.infra.api.monitor.vo;

import lombok.Data;

@Data
public class ServerJvmVO {

    private String name;
    private String vendor;
    private String version;
    private String specVersion;
    private String startTime;
    private String uptime;
    private Long uptimeMillis;
}
