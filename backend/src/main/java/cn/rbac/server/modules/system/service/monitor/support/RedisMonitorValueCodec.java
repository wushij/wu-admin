package cn.rbac.server.modules.system.service.monitor.support;

import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存监控读值：Redisson 写入的键带二进制前缀，StringRedisTemplate 直接读会乱码。
 */
public final class RedisMonitorValueCodec {

    /** 由 Redisson 维护、监控页应优先用 Redisson 解码的键前缀 */
    private static final List<String> REDISSON_MANAGED_PREFIXES = List.of(
            "dashboard:visit:date:",
            "dashboard:stats:aggregate:",
            "cache:sys:",
            "api:access:log:"
    );

    private RedisMonitorValueCodec() {
    }

    public static Object readValue(StringRedisTemplate stringRedisTemplate,
                                   RedissonClient redissonClient,
                                   @NonNull String key,
                                   @NonNull String type) {
        Object plain = readPlain(stringRedisTemplate, key, type);
        if (shouldTryRedissonDecode(key, plain)) {
            Object decoded = readViaRedisson(redissonClient, key, type);
            if (decoded != null) {
                return decoded;
            }
        }
        if (plain != null && !needsRedissonDecode(plain)) {
            return plain;
        }
        Object decoded = readViaRedisson(redissonClient, key, type);
        return decoded != null ? decoded : plain;
    }

    static boolean shouldTryRedissonDecode(String key, Object plain) {
        if (key != null) {
            for (String prefix : REDISSON_MANAGED_PREFIXES) {
                if (key.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return needsRedissonDecode(plain);
    }

    static boolean needsRedissonDecode(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return looksRedissonEncoded(s);
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof String s && looksRedissonEncoded(s)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                if (item instanceof String s && looksRedissonEncoded(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean looksRedissonEncoded(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        if (isReadableCacheText(raw)) {
            return false;
        }
        char first = raw.charAt(0);
        if (first == '\uFFFD' || first <= '\u0008') {
            return true;
        }
        int nonPrintable = 0;
        int sample = Math.min(raw.length(), 12);
        for (int i = 0; i < sample; i++) {
            char c = raw.charAt(i);
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                nonPrintable++;
            }
        }
        return nonPrintable >= 1 || raw.length() <= 16;
    }

    /** 纯文本 / JSON / 数字字符串，可直接展示 */
    static boolean isReadableCacheText(String raw) {
        String s = raw.strip();
        if (s.isEmpty()) {
            return true;
        }
        char first = s.charAt(0);
        if (first == '{' || first == '[' || first == '"') {
            return true;
        }
        if (first == '-' || (first >= '0' && first <= '9')) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    continue;
                }
                return false;
            }
            return true;
        }
        return (first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z');
    }

    private static Object readPlain(StringRedisTemplate template, @NonNull String key, @NonNull String type) {
        return switch (type) {
            case "string" -> template.opsForValue().get(key);
            case "list" -> template.opsForList().range(key, 0, -1);
            case "set" -> template.opsForSet().members(key);
            case "zset" -> template.opsForZSet().range(key, 0, -1);
            case "hash" -> template.opsForHash().entries(key);
            default -> null;
        };
    }

    private static Object readViaRedisson(RedissonClient redissonClient, String key, String type) {
        if (redissonClient == null) {
            return null;
        }
        try {
            return switch (type) {
                case "string" -> redissonClient.getBucket(key).get();
                case "list" -> readList(redissonClient, key);
                case "set" -> new ArrayList<>(redissonClient.getSet(key).readAll());
                case "zset" -> new ArrayList<>(redissonClient.getScoredSortedSet(key).valueRange(0, -1));
                case "hash" -> new LinkedHashMap<>(redissonClient.getMap(key).readAllMap());
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<Object> readList(RedissonClient redissonClient, String key) {
        try {
            return new ArrayList<>(redissonClient.getDeque(key).readAll());
        } catch (Exception ignored) {
            return new ArrayList<>(redissonClient.getList(key).readAll());
        }
    }
}
