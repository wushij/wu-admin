package cn.rbac.server.framework.security.core.service;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * 安全工具类
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getLoginUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        return null;
    }

    /**
     * 获取当前登录用户名（用于操作日志等展示，非 userId）
     */
    public static String getLoginUsername() {
        try {
            if (StpUtil.isLogin()) {
                Object usernameObj = StpUtil.getSession().get(TokenService.SESSION_USERNAME);
                if (usernameObj != null && StringUtils.hasText(usernameObj.toString())) {
                    return usernameObj.toString();
                }
            }
        } catch (Exception ignored) {
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        // principal 为 Long 时 getName() 仅为数字 ID，不能当作用户名
        if (name != null && name.matches("\\d+")) {
            return null;
        }
        return name;
    }
}
