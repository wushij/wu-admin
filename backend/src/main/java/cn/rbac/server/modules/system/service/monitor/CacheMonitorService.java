package cn.rbac.server.modules.system.service.monitor;

import cn.rbac.server.modules.system.api.monitor.vo.CacheInfoVO;
import cn.rbac.server.modules.system.api.monitor.vo.CacheKeysVO;
import cn.rbac.server.modules.system.api.monitor.vo.CacheStatsVO;
import cn.rbac.server.modules.system.api.monitor.vo.CacheValueVO;

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
