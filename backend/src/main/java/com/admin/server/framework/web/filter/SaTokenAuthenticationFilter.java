package com.admin.server.framework.web.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.framework.security.core.AuthTokenResolver;
import com.admin.server.framework.security.core.SecurityPathRules;
import com.admin.server.framework.web.core.ApiErrorResponseWriter;
import com.admin.server.framework.config.DynamicConfigProvider;
import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.framework.security.core.RiskScoringApi;
import com.admin.server.framework.web.core.OnlineUserTouchApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 统一登录态入口（替代 Spring Security 的 {@code anyRequest().authenticated()} 兜底）。
 * <p>路径规则与改造前 {@code SecurityConfig} 一致：忽略路径完全放行，
 * 其余路径要求 Sa-Token 已登录，未登录返回与原来相同的 401 JSON；
 * 已登录请求继续刷新在线用户最后活跃时间。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SaTokenAuthenticationFilter extends OncePerRequestFilter {

    private final OnlineUserTouchApi onlineUserService;
    private final ApiErrorResponseWriter errorResponseWriter;
    private final RiskScoringApi antiBotRiskService;
    private final DynamicConfigProvider dynamicConfigProvider;

    public SaTokenAuthenticationFilter(OnlineUserTouchApi onlineUserService,
                                       ApiErrorResponseWriter errorResponseWriter,
                                       RiskScoringApi antiBotRiskService,
                                       DynamicConfigProvider dynamicConfigProvider) {
        this.onlineUserService = onlineUserService;
        this.errorResponseWriter = errorResponseWriter;
        this.antiBotRiskService = antiBotRiskService;
        this.dynamicConfigProvider = dynamicConfigProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = SecurityPathRules.resolvePath(request);
        if (SecurityPathRules.isIgnored(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean loggedIn = false;
        // 过滤器链运行在 DispatcherServlet 之前，Sa-Token 的 Spring 适配层此时拿不到
        // RequestContextHolder，调用 StpUtil.isLogin() 会抛「非 web 上下文」。
        // 因此这里直接从请求头/Cookie 解析 Token，再用不依赖 Web 上下文的
        // getLoginIdByToken 校验登录态。
        String token = AuthTokenResolver.resolve(request);
        if (StringUtils.hasText(token)) {
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId != null) {
                    long userId = Long.parseLong(loginId.toString());
                    // 账号级风控封禁：命中则强制下线并拒绝，避免攻击者换 IP 后继续使用同一账号
                    if (isAccountBlocked(request, userId)) {
                        try {
                            StpUtil.logout(userId);
                        } catch (Exception ignored) {
                            // 下线失败不影响拒绝本次请求
                        }
                        errorResponseWriter.write(response, 403, "账号因异常行为被临时限制，请稍后再试");
                        return;
                    }
                    loggedIn = true;
                    // 在线活跃时间是旁路统计，不能因为 Redis 抖动把已登录用户误判成 401
                    try {
                        onlineUserService.touchLastAccess(userId);
                    } catch (Exception e) {
                        log.debug("刷新在线活跃时间失败 [{}]: {}", request.getRequestURI(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("Sa-Token 鉴权跳过 [{}]: {}", request.getRequestURI(), e.getMessage());
            }
        }

        if (!loggedIn && !SecurityPathRules.isPermitAll(request.getMethod(), path)) {
            errorResponseWriter.write(response, 401, "未登录或登录已过期");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 账号级风控封禁检查。
     *
     * <p>只查 {@code user:} 主体，不连带查 IP：IP 维度由 {@code AntiBotGateFilter} 负责，
     * 两处职责分开后，账号被封不会让同一办公网出口的所有人一起掉线。</p>
     */
    private boolean isAccountBlocked(HttpServletRequest request, long userId) {
        if (!dynamicConfigProvider.isRiskEnabled()) {
            return false;
        }
        try {
            return antiBotRiskService.isBlocked(
                    RiskPrincipal.ofUser(userId, ClientIpUtils.resolve(request)));
        } catch (Exception e) {
            log.warn("读取账号风控状态失败: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }
}
