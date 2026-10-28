package com.admin.server.framework.security.core.filter;

import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.framework.security.core.HoneypotPaths;
import com.admin.server.framework.web.core.ApiErrorResponseWriter;
import com.admin.server.framework.config.DynamicConfigProvider;
import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.framework.security.core.RiskPrincipalResolver;
import com.admin.server.framework.security.core.RiskScoringApi;
import com.admin.server.framework.security.core.SliderChallengeVerifier;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * 全路径风控门禁：IP 封禁、爬虫 UA 嗅探、行为识别与风控挑战。
 *
 * <p>与 {@link ApiSecurityFilter} 分离，确保即使请求命中签名/加密排除路径
 * （例如 {@code /files} 文件直出、{@code /ws}、{@code /doc.html}），
 * 也无法绕过封禁和 UA 风控。</p>
 *
 * <p>性能约束：门禁路径每个请求只做一次封禁查询；行为识别只在「带分页参数的 GET」
 * 和「导出/下载类路径」上触发，正常业务请求不产生额外 Redis 开销。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class AntiBotGateFilter extends OncePerRequestFilter {

    /** 健康检查、站点图标、错误页不参与嗅探，避免监控探针被误伤 */
    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/actuator",
            "/favicon.ico",
            "/error"
    );

    /** 分页参数名，兼容不同前端组件的写法 */
    private static final String[] PAGE_PARAM_NAMES = {"pageNo", "pageNum", "page", "currentPage"};

    /** 导出/下载类路径特征 */
    private static final String[] EXPORT_HINTS = {"/export", "/download"};

    /** 行为统计窗口（秒） */
    private static final int BEHAVIOR_WINDOW_SECONDS = 60;

    /** 同一主体同类行为事件的冷却时间（秒），避免一次遍历反复加分直接顶到封禁 */
    private static final int BEHAVIOR_COOLDOWN_SECONDS = 120;

    private static final String PAGE_SCAN_KEY_PREFIX = "security:risk:page:";
    private static final String EXPORT_KEY_PREFIX = "security:risk:export:";
    private static final String COOLDOWN_KEY_PREFIX = "security:risk:cd:";

    @Resource
    private RiskScoringApi antiBotRiskService;

    @Resource
    private ApiErrorResponseWriter errorResponseWriter;

    @Resource
    private DynamicConfigProvider dynamicConfigProvider;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SliderChallengeVerifier sliderCaptchaService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : SKIP_PREFIXES) {
            if (uri.startsWith(prefix) || uri.startsWith("/api" + prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = ClientIpUtils.resolve(request);

        // 风控总开关关闭即全部失效：已写入的封禁标记也不再拦截，避免应急关闭风控后仍有人被拒绝而误判
        boolean blocked;
        try {
            blocked = dynamicConfigProvider.isRiskEnabled() && antiBotRiskService.isBlocked(clientIp);
        } catch (Exception e) {
            // 风控存储故障时优先保证站点可用，不能让静态文件与登录页一起不可用
            log.warn("反爬门禁读取封禁状态失败，按放行处理: ip={}, reason={}", clientIp, e.getMessage());
            blocked = false;
        }
        if (blocked) {
            errorResponseWriter.write(response, 403,
                    "请求已被拒绝：当前网络环境风险过高已被系统临时限制，请稍后再试");
            return;
        }

        // 蜜罐路径不做 UA 拦截：让请求真正落到蜜罐处理器，
        // 以便记录 HONEYPOT 事件并按蜜罐策略封禁，而不是笼统记成 UA_BLOCK。
        // 此前的 IP 封禁检查仍然生效。
        if (HoneypotPaths.matches(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String userAgent = request.getHeader("User-Agent");
        String matchedUa = antiBotRiskService.matchBlockedUserAgent(userAgent);
        if (matchedUa != null) {
            try {
                RiskPrincipal subject = RiskPrincipalResolver.resolve(request);
                antiBotRiskService.addRisk(subject, 40, "UA_BLOCK", "BLOCKED",
                        request.getRequestURI(), request.getMethod(), userAgent,
                        "命中自动化脚本/爬虫指纹: " + matchedUa);
            } catch (Exception e) {
                log.warn("记录 UA 风险分失败: ip={}, reason={}", clientIp, e.getMessage());
            }
            errorResponseWriter.write(response, 403, "请求已被拒绝：检测到自动化爬虫工具访问");
            return;
        }

        if (dynamicConfigProvider.isRiskEnabled() && dynamicConfigProvider.isRiskBehaviorEnabled()) {
            try {
                inspectBehavior(request, clientIp, userAgent);
            } catch (Exception e) {
                // 行为识别是增强能力，异常不能把正常请求拦下来
                log.warn("行为风控执行失败: ip={}, reason={}", clientIp, e.getMessage());
            }
        }

        if (rejectIfChallengeRequired(request, response, clientIp, userAgent)) {
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 行为识别：翻页遍历与批量导出。
     *
     * <p>只对「GET + 分页参数」和「导出/下载路径」生效，其余请求直接返回。</p>
     */
    private void inspectBehavior(HttpServletRequest request, String clientIp, String userAgent) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method)) {
            return;
        }
        String uri = request.getRequestURI();

        if (isExportPath(uri)) {
            long count = recordAndCount(EXPORT_KEY_PREFIX, clientIp, "export");
            int threshold = dynamicConfigProvider.getRiskExportThreshold();
            if (count >= threshold && acquireCooldown(clientIp, "export")) {
                RiskPrincipal subject = RiskPrincipalResolver.resolve(request);
                antiBotRiskService.addRisk(subject, 30, "BULK_EXPORT", "WARNING",
                        uri, method, userAgent,
                        "批量导出/下载异常：" + BEHAVIOR_WINDOW_SECONDS + " 秒内 " + count + " 次");
            }
            return;
        }

        String pageValue = firstPresentParameter(request, PAGE_PARAM_NAMES);
        if (pageValue == null) {
            return;
        }
        long pageNo = parseLong(pageValue);
        if (pageNo <= 0) {
            return;
        }
        // 成员用页码本身，同一页反复刷新不会重复计数，只有真正往后翻才算遍历
        long distinctPages = recordAndCount(PAGE_SCAN_KEY_PREFIX, clientIp, String.valueOf(pageNo));
        int threshold = dynamicConfigProvider.getRiskPageScanThreshold();
        if (distinctPages >= threshold && acquireCooldown(clientIp, "page")) {
            RiskPrincipal subject = RiskPrincipalResolver.resolve(request);
            antiBotRiskService.addRisk(subject, 25, "PAGE_SCAN", "WARNING",
                    uri, method, userAgent,
                    "翻页遍历异常：" + BEHAVIOR_WINDOW_SECONDS + " 秒内访问 " + distinctPages + " 个不同页码");
        }
    }

    /**
     * 记录一次行为并返回窗口内的去重计数。
     */
    private long recordAndCount(String prefix, String principal, String member) {
        String key = prefix + principal;
        long now = System.currentTimeMillis();
        long from = now - BEHAVIOR_WINDOW_SECONDS * 1000L;
        RScoredSortedSet<String> set = redissonClient.getScoredSortedSet(key);
        set.add(now, member);
        set.expire(Duration.ofSeconds(BEHAVIOR_WINDOW_SECONDS + 60L));
        set.removeRangeByScore(Double.NEGATIVE_INFINITY, true, (double) from, false);
        return set.count((double) from, true, (double) now, true);
    }

    /**
     * 同类事件冷却：返回 true 表示本次允许记分。
     */
    private boolean acquireCooldown(String principal, String type) {
        try {
            return redissonClient.getBucket(COOLDOWN_KEY_PREFIX + type + ":" + principal)
                    .setIfAbsent("1", Duration.ofSeconds(BEHAVIOR_COOLDOWN_SECONDS));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 风控挑战：开启后，达到挑战等级的主体发起写操作时必须先完成安全验证。
     */
    private boolean rejectIfChallengeRequired(HttpServletRequest request, HttpServletResponse response,
                                              String clientIp, String userAgent) throws IOException {
        // 总开关关闭时历史风险分不再触发挑战
        if (!dynamicConfigProvider.isRiskEnabled() || !dynamicConfigProvider.isRiskChallengeEnabled()) {
            return false;
        }
        if (!isWriteMethod(request.getMethod())) {
            return false;
        }
        RiskPrincipal subject;
        try {
            subject = RiskPrincipalResolver.resolve(request);
        } catch (Exception e) {
            return false;
        }
        if (!antiBotRiskService.isChallengeRequired(subject)) {
            return false;
        }
        // 前端完成滑块验证后重放原请求，携带 X-Slider-Ticket 作为一次性豁免凭证
        String ticket = request.getHeader("X-Slider-Ticket");
        if (ticket != null && !ticket.isBlank()) {
            String error = sliderCaptchaService.verifyAndConsume(ticket.trim(), null, null, clientIp);
            if (error == null) {
                antiBotRiskService.markChallengePassed(subject);
                return false;
            }
        }
        try {
            if (acquireCooldown(clientIp, "challenge")) {
                antiBotRiskService.addRisk(subject, 0, "RISK_CHALLENGE", "CHALLENGE",
                        request.getRequestURI(), request.getMethod(), userAgent,
                        "高风险主体发起写操作，要求完成安全验证");
            }
        } catch (Exception ignored) {
            // 记录失败也要继续拦截
        }
        response.setHeader("X-Risk-Challenge", "slide");
        errorResponseWriter.write(response, 403, "当前操作触发风控校验，请完成安全验证后重试");
        return true;
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private boolean isExportPath(String uri) {
        if (uri == null) {
            return false;
        }
        String lower = uri.toLowerCase();
        for (String hint : EXPORT_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private String firstPresentParameter(HttpServletRequest request, String[] names) {
        for (String name : names) {
            String value = request.getParameter(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private long parseLong(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
