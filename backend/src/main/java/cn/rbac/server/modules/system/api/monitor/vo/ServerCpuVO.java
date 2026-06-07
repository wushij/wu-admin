package cn.rbac.server.modules.system.api.monitor.vo;

import lombok.Data;

@Data
public class ServerCpuVO {

    private String name;
    private String arch;
    private Integer availableProcessors;
    /** 系统平均负载（Linux 有效；Windows 常为 -1） */
    private Double systemLoadAverage;
    /** 系统 CPU 使用率 0~100，不支持时为 null */
    private Double systemCpuPercent;
    /** 当前 Java 进程 CPU 使用率 0~100，不支持时为 null */
    private Double processCpuPercent;
}
