package com.admin.server.framework.security.core;

import cn.dev33.satoken.stp.StpUtil;
import com.admin.server.common.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 从请求解析风险主体。
 *
 * <p>只在需要记分时调用：解析登录态需要读一次 Sa-Token，
 * 不能放进每个请求都要走的门禁路径上，否则会给所有请求增加一次 Redis 往返。</p>
 */
@Slf4j
public final class RiskPrincipalResolver {

    private RiskPrincipalResolver() {
    }

    public static RiskPrincipal resolve(HttpServletRequest request) {
        if (request == null) {
            return RiskPrincipal.ofIp(null);
        }
        String ip = ClientIpUtils.resolve(request);
        Long userId = resolveUserId(request);
        return userId == null ? RiskPrincipal.ofIp(ip) : RiskPrincipal.ofUser(userId, ip);
    }

    /**
     * 解析当前登录用户 ID。
     *
     * <p>风控过滤器运行在 DispatcherServlet 之前，此时 Sa-Token 的 Web 上下文尚未建立，
     * 因此不能用 {@code StpUtil.isLogin()}，只能拿 Token 反查登录 ID，
     * 与 {@code SaTokenAuthenticationFilter} 的处理方式保持一致。</p>
     */
    public static Long resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        try {
            String token = AuthTokenResolver.resolve(request);
            if (!StringUtils.hasText(token)) {
                return null;
            }
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }
            return Long.parseLong(loginId.toString());
        } catch (Exception e) {
            // 未登录、Token 失效、Redis 抖动都会走到这里，按匿名处理即可
            log.debug("解析风险主体登录态失败: {}", e.getMessage());
            return null;
        }
    }
}
