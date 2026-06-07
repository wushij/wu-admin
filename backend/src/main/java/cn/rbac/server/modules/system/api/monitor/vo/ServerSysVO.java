package cn.rbac.server.modules.system.api.monitor.vo;

import lombok.Data;

@Data
public class ServerSysVO {

    private String hostName;
    private String hostAddress;
    private String osName;
    private String osVersion;
    private String userDir;
    private String javaVersion;
}
