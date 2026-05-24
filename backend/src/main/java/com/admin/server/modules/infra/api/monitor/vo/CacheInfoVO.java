package com.admin.server.modules.infra.api.monitor.vo;

import lombok.Data;

/**
 * Redis 服务信息（概览面板）
 */
@Data
public class CacheInfoVO {

    private String redisVersion;
    private String redisMode;
    private String os;
    private Integer tcpPort;
    private Long uptimeInDays;
    private Long connectedClients;
    private Long dbSize;
    private String usedMemoryHuman;
    private String usedMemoryPeakHuman;
    private Long totalCommandsProcessed;
    private Long instantaneousOpsPerSec;
}
