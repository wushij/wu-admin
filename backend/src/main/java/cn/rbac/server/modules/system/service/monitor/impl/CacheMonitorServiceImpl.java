package cn.rbac.server.modules.system.service.monitor.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.api.monitor.vo.CacheInfoVO;
import cn.rbac.server.modules.system.api.monitor.vo.CacheKeyItemVO;
import cn.rbac.server.modules.system.api.monitor.vo.CacheKeysVO;
import cn.rbac.server.modules.system.api.monitor.vo.CacheStatsVO;
import cn.rbac.server.modules.system.api.monitor.vo.CacheValueVO;
import cn.rbac.server.modules.system.service.monitor.CacheMonitorService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CacheMonitorServiceImpl implements CacheMonitorService {

    private static final int MAX_SCAN_LIMIT = 500;

    /** 禁止通过监控页删除的关键缓存前缀（会话、认证、系统配置等） */
    private static final List<String> PROTECTED_KEY_PREFIXES = List.of(
            "Authorization:",
            "satoken:",
            "captcha:",
            "sms:",
            "auth:",
            "cache:sys:",
            "api:access:log:"
    );

    /** 上次采样的 keyspace 计数，用于计算周期命中率 */
    private final AtomicLong lastKeyspaceHits = new AtomicLong(-1);
    private final AtomicLong lastKeyspaceMisses = new AtomicLong(-1);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public CacheStatsVO getStats() {
        Properties info = redisInfo();
        CacheStatsVO vo = new CacheStatsVO();
        if (info == null) {
            return vo;
        }
        long usedMemory = parseLong(info.getProperty("used_memory"));
        long configuredMax = parseLong(info.getProperty("maxmemory"));
        long systemMemory = parseLong(info.getProperty("total_system_memory"));
        boolean maxMemoryConfigured = configuredMax > 0;

        long chartMax = configuredMax;
        if (!maxMemoryConfigured && systemMemory > 0) {
            chartMax = systemMemory;
        }

        long hits = parseLong(info.getProperty("keyspace_hits"));
        long misses = parseLong(info.getProperty("keyspace_misses"));

        Double periodHitRate = resolvePeriodHitRate(hits, misses);
        Double cumulativeHitRate = (hits + misses) == 0 ? null : (double) hits / (hits + misses);

        vo.setUsedMemory(usedMemory);
        vo.setMaxMemory(chartMax);
        vo.setMaxMemoryConfigured(maxMemoryConfigured);
        vo.setSystemMemory(systemMemory);
        vo.setUsedMemoryHuman(info.getProperty("used_memory_human", "0"));
        vo.setOps(parseLong(info.getProperty("instantaneous_ops_per_sec")));
        vo.setHitRate(periodHitRate);
        vo.setCumulativeHitRate(cumulativeHitRate);
        vo.setKeyspaceHits(hits);
        vo.setKeyspaceMisses(misses);
        vo.setConnectedClients(parseLong(info.getProperty("connected_clients")));
        return vo;
    }

    /**
     * 相对上次 {@link #getStats()} 调用的增量命中率；首次采样或无读写返回 null
     */
    private Double resolvePeriodHitRate(long hits, long misses) {
        long prevHits = lastKeyspaceHits.getAndSet(hits);
        long prevMisses = lastKeyspaceMisses.getAndSet(misses);
        if (prevHits < 0) {
            return null;
        }
        long deltaHits = hits - prevHits;
        long deltaMisses = misses - prevMisses;
        if (deltaHits + deltaMisses <= 0) {
            return null;
        }
        return (double) deltaHits / (deltaHits + deltaMisses);
    }

    @Override
    public CacheInfoVO getInfo() {
        Properties info = redisInfo();
        CacheInfoVO vo = new CacheInfoVO();
        if (info == null) {
            return vo;
        }
        vo.setRedisVersion(info.getProperty("redis_version", "unknown"));
        vo.setRedisMode(info.getProperty("redis_mode", "standalone"));
        vo.setOs(info.getProperty("os", ""));
        vo.setTcpPort(parseInt(info.getProperty("tcp_port")));
        vo.setUptimeInDays(parseLong(info.getProperty("uptime_in_days")));
        vo.setConnectedClients(parseLong(info.getProperty("connected_clients")));
        vo.setUsedMemoryHuman(info.getProperty("used_memory_human", "0"));
        vo.setUsedMemoryPeakHuman(info.getProperty("used_memory_peak_human", "0"));
        vo.setTotalCommandsProcessed(parseLong(info.getProperty("total_commands_processed")));
        vo.setInstantaneousOpsPerSec(parseLong(info.getProperty("instantaneous_ops_per_sec")));

        Long dbSize = stringRedisTemplate.execute((RedisCallback<Long>) connection ->
                connection.serverCommands().dbSize());
        vo.setDbSize(dbSize != null ? dbSize : 0L);
        return vo;
    }

    @Override
    public CacheKeysVO scanKeys(String pattern, int limit) {
        String match = Objects.requireNonNull(StringUtils.hasText(pattern) ? pattern.trim() : "*");
        int cap = Math.min(Math.max(limit, 1), MAX_SCAN_LIMIT);

        List<String> keys = new ArrayList<>();
        boolean truncated = false;
        ScanOptions options = ScanOptions.scanOptions().match(match).count(100).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                if (keys.size() >= cap) {
                    truncated = true;
                    break;
                }
                keys.add(cursor.next());
            }
        }
        keys.sort(Comparator.naturalOrder());

        List<CacheKeyItemVO> items = enrichKeyItems(keys);

        CacheKeysVO vo = new CacheKeysVO();
        vo.setItems(items);
        vo.setCount(items.size());
        vo.setLimit(cap);
        vo.setTruncated(truncated);
        return vo;
    }

    @Override
    public CacheValueVO getValue(String key) {
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(400, "键名不能为空");
        }
        String cacheKey = Objects.requireNonNull(key.trim());
        DataType dataType = stringRedisTemplate.type(cacheKey);
        if (dataType == null || DataType.NONE.equals(dataType)) {
            throw new BusinessException(404, "缓存键不存在");
        }
        String type = dataType.code();
        Long ttl = stringRedisTemplate.getExpire(cacheKey);
        Object value = readValueByType(cacheKey, type);

        CacheValueVO vo = new CacheValueVO();
        vo.setKey(cacheKey);
        vo.setType(type);
        vo.setTtl(ttl);
        vo.setValue(value);
        return vo;
    }

    @Override
    public void deleteKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(400, "键名不能为空");
        }
        assertDeletable(key.trim());
        Boolean deleted = stringRedisTemplate.delete(key);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new BusinessException(404, "缓存键不存在或已被删除");
        }
    }

    private void assertDeletable(String key) {
        for (String prefix : PROTECTED_KEY_PREFIXES) {
            if (key.startsWith(prefix)) {
                throw new BusinessException(403, "不允许删除系统关键缓存键（前缀: " + prefix + "）");
            }
        }
    }

    private List<CacheKeyItemVO> enrichKeyItems(List<String> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        List<Object> pipelineResults = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                byte[] raw = stringRedisTemplate.getStringSerializer().serialize(Objects.requireNonNull(key));
                if (raw != null) {
                    connection.keyCommands().type(raw);
                    connection.keyCommands().ttl(raw);
                }
            }
            return null;
        });

        List<CacheKeyItemVO> items = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            CacheKeyItemVO item = new CacheKeyItemVO();
            item.setKey(keys.get(i));
            int base = i * 2;
            item.setType(resolvePipelineType(pipelineResults, base));
            item.setTtl(resolvePipelineTtl(pipelineResults, base + 1));
            items.add(item);
        }
        return items;
    }

    private String resolvePipelineType(List<Object> results, int index) {
        if (index >= results.size() || results.get(index) == null) {
            return "unknown";
        }
        Object raw = results.get(index);
        if (raw instanceof DataType dataType) {
            return dataType.code();
        }
        return String.valueOf(raw);
    }

    private long resolvePipelineTtl(List<Object> results, int index) {
        if (index >= results.size() || results.get(index) == null) {
            return -2L;
        }
        Object raw = results.get(index);
        if (raw instanceof Long ttl) {
            return ttl;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return -2L;
    }

    private Object readValueByType(String key, String type) {
        String cacheKey = Objects.requireNonNull(key);
        return switch (type) {
            case "string" -> stringRedisTemplate.opsForValue().get(cacheKey);
            case "list" -> stringRedisTemplate.opsForList().range(cacheKey, 0, -1);
            case "set" -> stringRedisTemplate.opsForSet().members(cacheKey);
            case "zset" -> stringRedisTemplate.opsForZSet().range(cacheKey, 0, -1);
            case "hash" -> stringRedisTemplate.opsForHash().entries(cacheKey);
            default -> null;
        };
    }

    private Properties redisInfo() {
        return stringRedisTemplate.execute((RedisCallback<Properties>) connection ->
                connection.serverCommands().info());
    }

    private long parseLong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private int parseInt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
