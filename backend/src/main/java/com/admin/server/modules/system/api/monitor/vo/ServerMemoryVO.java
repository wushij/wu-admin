package com.admin.server.modules.system.api.monitor.vo;

import lombok.Data;

@Data
public class ServerMemoryVO {

    private String heapInit;
    private String heapUsed;
    private String heapMax;
    private String heapCommitted;
    private String nonHeapUsed;
    private Long heapUsedBytes;
    private Long heapMaxBytes;
    /** JVM 堆使用率 0~100 */
    private Double heapUsedPercent;
    private String physicalTotal;
    private String physicalFree;
    private Double physicalUsedPercent;
}
