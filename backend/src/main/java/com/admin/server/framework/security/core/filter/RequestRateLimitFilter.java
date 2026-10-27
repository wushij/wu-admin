package com.admin.server.framework.security.core.filter;

import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.framework.web.core.ApiErrorResponseWriter;
import com.admin.server.framework.config.DynamicConfigProvider;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 应用层滑动窗口限流。
 *
 * <p>Nginx 的 {@code limit_req} 只按 IP 做了较宽的粗粒度限流，且依赖部署配置；
 * 这里在应用内再按「IP + 路由分组」做一层兜底，避免单 IP 被爬虫高速拉取业务接口。
 * 采用 Redis ZSET 滑动窗口，窗口固定 60 秒，超限返回 429。</p>
 *
 * <p>关键实现约束：清理、计数、写入必须在同一段 Lua 中原子完成。
 * 之前用 {@code removeRangeByScore → add → zCard} 三次独立调用，并发请求会互相
 * 看到中间状态，实际放行量会明显超过配置上限（高并发限流失效）。</p>
 *
 * <p>与 {@link AntiBotGateFilter} 分离：封禁/UA 风控先执行，本过滤器只做频率控制。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 8)
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60L;

    /**
     * 滑动窗口原子脚本：返回窗口内当前请求数，调用方据此判断是否超限。
     * <ul>
     *   <li>KEYS[1] 限流键</li>
     *   <li>ARGV[1] 当前毫秒时间戳</li>
     *   <li>ARGV[2] 窗口起点毫秒（now - 60000）</li>
     *   <li>ARGV[3] 请求唯一标识（避免同毫秒成员覆盖）</li>
     *   <li>ARGV[4] 键过期秒数</li>
     * </ul>
     */
    private static final RedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local cutoff = tonumber(ARGV[2])
            local member = ARGV[3]
            local ttl = tonumber(ARGV[4])
            redis.call('ZREMRANGEBYSCORE', key, 0, cutoff)
            redis.call('ZADD', key, now, member)
            local count = redis.call('ZCARD', key)
            redis.call('EXPIRE', key, ttl)
            return count
            """, Long.class);

    /** 静态资源、文档、WebSocket 握手和 Nginx 直出文件不参与应用层限流 */
    private static final Set<String> EXCLUDE_PREFIXES = Set.of(
            "/api/doc.html",
            "/api/v3/api-docs",
            "/api/swagger-ui",
            "/api/webjars",
            "/api/actuator",
            "/api/favicon.ico",
            "/api/error",
            "/api/ws",
            "/api/files/"
    );

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DynamicConfigProvider dynamicConfigProvider;

    @Resource
    private ApiErrorResponseWriter errorResponseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        for (String prefix : EXCLUDE_PREFIXES) {
            if (uri.startsWith(prefix) || uri.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = ClientIpUtils.resolve(request);
        String group = resolveGroup(request.getRequestURI());
        int limit = resolveLimit(group);
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean allowed;
        try {
            allowed = slidingWindowAllow("security:rate:" + group + ":" + clientIp, limit);
        } catch (Exception e) {
            // 限流存储故障时优先保证可用性，与 AntiBotGateFilter 的 fail-open 策略保持一致
            log.warn("应用层限流读取失败，按放行处理: ip={}, group={}, reason={}",
                    clientIp, group, e.getMessage());
            allowed = true;
        }

        if (!allowed) {
            errorResponseWriter.write(response, 429, "请求过于频繁，请稍后再试");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String resolveGroup(String uri) {
        if (uri.startsWith("/api/auth/")) {
            return "auth";
        }
        if (uri.startsWith("/api/storage/files/")) {
            return "files";
        }
        return "api";
    }

    private int resolveLimit(String group) {
        return switch (group) {
            case "auth" -> dynamicConfigProvider.getAuthPerIpMinute();
            case "files" -> dynamicConfigProvider.getFilesPerIpMinute();
            default -> dynamicConfigProvider.getApiPerIpMinute();
        };
    }

    private boolean slidingWindowAllow(String key, int limit) {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_SECONDS * 1000L;
        Long count = stringRedisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                List.of(key),
                String.valueOf(now),
                String.valueOf(cutoff),
                UUID.randomUUID().toString(),
                String.valueOf(WINDOW_SECONDS + 60));
        // 脚本返回 null 说明 Redis 侧未返回计数（极少见），按放行处理避免误伤正常用户
        return count == null || count <= limit;
    }
}
