package com.admin.server.modules.infra.api.monitor.vo;

import lombok.Data;

/**
 * 单个缓存键详情
 */
@Data
public class CacheValueVO {

    private String key;
    private String type;
    /** 秒；-1 永久；-2 不存在 */
    private Long ttl;
    private Object value;
}
