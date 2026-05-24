package com.admin.server.modules.infra.api.monitor.vo;

import lombok.Data;

/**
 * Redis 实时统计（图表轮询）
 */
@Data
public class CacheStatsVO {

    private Long usedMemory;
    /** 图表分母：优先 maxmemory，否则 total_system_memory；均为 0 时前端仅展示占用量 */
    private Long maxMemory;
    /** Redis 是否配置了 maxmemory 上限 */
    private Boolean maxMemoryConfigured;
    /** 主机总内存（Redis INFO total_system_memory） */
    private Long systemMemory;
    private String usedMemoryHuman;
    private Long ops;
    /**
     * 本采样周期命中率（相对上次 stats 的增量），0~1；无读写时为 null
     */
    private Double hitRate;
    /** 累计命中率（keyspace_hits / (hits+misses)），无统计时为 null */
    private Double cumulativeHitRate;
    private Long keyspaceHits;
    private Long keyspaceMisses;
    private Long connectedClients;
}
