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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 统一 Sa-Token 会话服务
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
     * 登录并返回 token（写入 Sa-Token Redis）
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
            String token = resolveTokenByLoginId(userId);
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

    /** 多端并发登录时优先取 token 列表中的有效项 */
    private String resolveTokenByLoginId(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            List<String> tokens = StpUtil.getTokenValueListByLoginId(userId);
            if (tokens != null) {
                for (String t : tokens) {
                    if (StringUtils.hasText(t) && StpUtil.getLoginIdByToken(t) != null) {
                        return t;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            String token = StpUtil.getTokenValueByLoginId(userId);
            if (StringUtils.hasText(token)) {
                return token;
            }
        } catch (Exception ignored) {
        }
        return null;
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

    /**
     * 列出当前已登录用户 ID（优先 searchSessionId，兼容 searchTokenValue）
     */
    public List<Long> listActiveUserIds() {
        Set<Long> ids = new LinkedHashSet<>();
        try {
            List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);
            if (sessionIds != null) {
                for (String sid : sessionIds) {
                    if (!StringUtils.hasText(sid)) {
                        continue;
                    }
                    try {
                        ids.add(Long.parseLong(sid.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.debug("searchSessionId failed: {}", e.getMessage());
        }
        if (ids.isEmpty()) {
            for (String token : listActiveTokens()) {
                Long userId = getUserId(token);
                if (userId != null) {
                    ids.add(userId);
                }
            }
        }
        return new ArrayList<>(ids);
    }
}
