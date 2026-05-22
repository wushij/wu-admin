package cn.rbac.server.framework.security.core.service;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.rbac.server.framework.config.DynamicConfigProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一 Sa-Token 会话服务（与网关共用 Redis，同一 Authorization token）
 */
@Slf4j
@Service
public class TokenService {

    public static final String SESSION_USERNAME = "username";
    public static final String SESSION_NICKNAME = "nickname";

    @Resource
    private DynamicConfigProvider dynamicConfigProvider;

    @Data
    public static class LoginInfo {
        private Long userId;
        private String username;
        private String token;
        private Long createTime;
        private Long expireTime;
    }

    private long timeoutSeconds() {
        return dynamicConfigProvider.getTokenExpirationMs() / 1000L;
    }

    /**
     * 登录并返回 token（写入 Sa-Token Redis，网关可直接校验）
     */
    public String createToken(Long userId, String username) {
        try {
            StpUtil.logout(userId);
        } catch (Exception ignored) {
        }
        SaLoginModel model = new SaLoginModel()
                .setTimeout(timeoutSeconds())
                .setIsLastingCookie(false);
        StpUtil.login(userId, model);
        StpUtil.getSession().set(SESSION_USERNAME, username);
        String token = StpUtil.getTokenValue();
        log.info("Sa-Token created for user: {}", username);
        return token;
    }

    public Long getUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }
            return Long.parseLong(loginId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public String getUsername(String token) {
        Long userId = getUserId(token);
        if (userId == null) {
            return null;
        }
        LoginInfo info = getLoginInfo(userId);
        return info != null ? info.getUsername() : null;
    }

    public boolean validateToken(String token) {
        return getUserId(token) != null;
    }

    public LoginInfo getLoginInfo(Long userId) {
        try {
            String token = StpUtil.getTokenValueByLoginId(userId);
            if (!StringUtils.hasText(token)) {
                return null;
            }
            LoginInfo loginInfo = new LoginInfo();
            loginInfo.setUserId(userId);
            loginInfo.setToken(token);
            Object username = StpUtil.getSessionByLoginId(userId).get(SESSION_USERNAME);
            loginInfo.setUsername(username != null ? username.toString() : "");
            long timeout = StpUtil.getTokenTimeout(token);
            long now = System.currentTimeMillis();
            loginInfo.setCreateTime(now);
            loginInfo.setExpireTime(timeout > 0 ? now + timeout * 1000L : now);
            return loginInfo;
        } catch (Exception e) {
            return null;
        }
    }

    public void removeToken(Long userId) {
        try {
            StpUtil.logout(userId);
        } catch (Exception ignored) {
        }
    }

    /**
     * 列出当前有效 token（用于在线用户）
     */
    public List<String> listActiveTokens() {
        List<String> tokens = new ArrayList<>();
        try {
            List<String> found = StpUtil.searchTokenValue("", 0, -1, false);
            if (found != null) {
                tokens.addAll(found);
            }
        } catch (Exception e) {
            log.debug("searchTokenValue failed: {}", e.getMessage());
        }
        return tokens;
    }
}
