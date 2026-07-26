package com.admin.server.modules.infra.service.monitor;

import com.admin.server.modules.infra.api.monitor.vo.CacheInfoVO;
import com.admin.server.modules.infra.api.monitor.vo.CacheKeysVO;
import com.admin.server.modules.infra.api.monitor.vo.CacheStatsVO;
import com.admin.server.modules.infra.api.monitor.vo.CacheValueVO;

/**
 * Redis 缓存监控
 */
public interface CacheMonitorService {

    CacheStatsVO getStats();

    CacheInfoVO getInfo();

    CacheKeysVO scanKeys(String pattern, int limit);

    CacheValueVO getValue(String key);

    void deleteKey(String key);
}
