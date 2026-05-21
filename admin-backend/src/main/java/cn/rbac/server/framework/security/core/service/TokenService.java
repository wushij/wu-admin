package cn.rbac.server.framework.security.core.service;

import cn.hutool.json.JSONUtil;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务
 */
@Slf4j
@Service
public class TokenService {

    @Value("${jwt.secret:rbac-secret-key-2026-rbac-secret-key-2026}")
    private String secret;

    @Resource
    private SystemConfigHelper systemConfigHelper;

    private final RedissonClient redissonClient;

    public TokenService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private long expirationMs() {
        return systemConfigHelper.getTokenExpirationMs();
    }

    private static final String TOKEN_PREFIX = "rbac:token:";

    /**
     * Redis 中存储的登录信息
     */
    @Data
    public static class LoginInfo {
        private Long userId;
        private String username;
        private String token;
        private Long createTime;
        private Long expireTime;
    }

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 创建 Token
     */
    public String createToken(Long userId, String username) {
        Date now = new Date();
        long expireMs = expirationMs();
        Date expiryDate = new Date(now.getTime() + expireMs);

        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // 构建易读的登录信息存储到 Redis
        try {
            LoginInfo loginInfo = new LoginInfo();
            loginInfo.setUserId(userId);
            loginInfo.setUsername(username);
            loginInfo.setToken(token);
            loginInfo.setCreateTime(now.getTime());
            loginInfo.setExpireTime(expiryDate.getTime());

            String key = TOKEN_PREFIX + userId;
            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(JSONUtil.toJsonStr(loginInfo), expireMs, TimeUnit.MILLISECONDS);
            log.info("Token created for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to store token in Redis for user: {}", username, e);
            throw new RuntimeException("Token存储失败", e);
        }

        return token;
    }

    /**
     * 从 Token 获取用户ID
     */
    public Long getUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 获取用户名
     */
    public String getUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("username", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Long userId = getUserId(token);
            if (userId == null) {
                return false;
            }
            // 检查 Redis 中的 token 是否匹配
            LoginInfo loginInfo = getLoginInfo(userId);
            if (loginInfo == null) {
                return false;
            }
            return token.equals(loginInfo.getToken());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取登录信息
     */
    public LoginInfo getLoginInfo(Long userId) {
        try {
            String key = TOKEN_PREFIX + userId;
            RBucket<String> bucket = redissonClient.getBucket(key);
            String json = bucket.get();
            if (json == null) {
                return null;
            }
            return JSONUtil.toBean(json, LoginInfo.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 移除 Token（登出）
     */
    public void removeToken(Long userId) {
        String key = TOKEN_PREFIX + userId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.delete();
    }
}
