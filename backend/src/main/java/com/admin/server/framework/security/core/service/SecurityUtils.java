package com.admin.server.framework.security.core.service;

import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * 安全工具类
 */
public class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

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
        } catch (Exception e) {
            log.debug("getLoginUserId via Sa-Token failed: {}", e.getMessage());
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
     * 获取当前登录用户ID，未登录时返回 0L（用于 Controller 层替代重复的 currentUserId() 实现）
     */
    public static Long getLoginUserIdOrZero() {
        Long userId = getLoginUserId();
        return userId != null ? userId : 0L;
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
        } catch (Exception e) {
            log.debug("getLoginUsername via Sa-Token failed: {}", e.getMessage());
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

    /**
     * 获取当前登录用户展示名（优先昵称，无昵称时回退用户名）
     */
    public static String getLoginUserDisplayName() {
        try {
            if (StpUtil.isLogin()) {
                Object nicknameObj = StpUtil.getSession().get(TokenService.SESSION_NICKNAME);
                if (nicknameObj != null && StringUtils.hasText(nicknameObj.toString())) {
                    return nicknameObj.toString().trim();
                }
                Object usernameObj = StpUtil.getSession().get(TokenService.SESSION_USERNAME);
                if (usernameObj != null && StringUtils.hasText(usernameObj.toString())) {
                    return usernameObj.toString().trim();
                }
            }
        } catch (Exception e) {
            log.debug("getLoginUserDisplayName via Sa-Token failed: {}", e.getMessage());
        }
        String name = getLoginUsername();
        return name;
    }
}
