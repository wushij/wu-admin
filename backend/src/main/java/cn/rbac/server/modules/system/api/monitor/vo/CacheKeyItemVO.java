package cn.rbac.server.modules.system.api.monitor.vo;

import lombok.Data;

/**
 * SCAN 单条缓存键摘要
 */
@Data
public class CacheKeyItemVO {

    private String key;
    private String type;
    /** 剩余秒数；-1 永久，-2 不存在 */
    private Long ttl;
}
