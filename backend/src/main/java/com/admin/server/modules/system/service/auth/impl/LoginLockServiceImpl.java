package com.admin.server.modules.system.service.auth.impl;

import com.admin.server.modules.system.service.auth.LoginLockService;
import com.admin.server.modules.system.service.auth.vo.LoginLockStatusVO;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class LoginLockServiceImpl implements LoginLockService {

    private static final String LOGIN_FAIL_USER_KEY = "auth:login:fail:user:";
    private static final String LOGIN_FAIL_IP_KEY = "auth:login:fail:ip:";
    private static final String LOGIN_LOCK_USER_KEY = "auth:login:lock:user:";
    private static final String LOGIN_LOCK_IP_KEY = "auth:login:lock:ip:";

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Override
    public String checkLoginLockMessage(String username, String ip) {
        String userLockMsg = getLockMessage(LOGIN_LOCK_USER_KEY + username, "账号");
        if (userLockMsg != null) {
            return userLockMsg;
        }
        return getLockMessage(LOGIN_LOCK_IP_KEY + ip, "IP");
    }

    @Override
    public void recordLoginFailure(String username, String ip) {
        long lockMinutes = systemConfigHelper.getLockTimeMinutes();
        increaseFailAndLock(
                LOGIN_FAIL_USER_KEY + username,
                LOGIN_LOCK_USER_KEY + username,
                systemConfigHelper.getMaxRetryCount(),
                lockMinutes);
        increaseFailAndLock(
                LOGIN_FAIL_IP_KEY + ip,
                LOGIN_LOCK_IP_KEY + ip,
                systemConfigHelper.getMaxRetryCountIp(),
                lockMinutes);
    }

    @Override
    public void clearLoginFailure(String username, String ip) {
        redissonClient.getBucket(LOGIN_FAIL_USER_KEY + username).delete();
        redissonClient.getBucket(LOGIN_FAIL_IP_KEY + ip).delete();
        redissonClient.getBucket(LOGIN_LOCK_USER_KEY + username).delete();
        redissonClient.getBucket(LOGIN_LOCK_IP_KEY + ip).delete();
    }

    @Override
    public LoginLockStatusVO getUserLockStatus(String username) {
        if (!StringUtils.hasText(username)) {
            return LoginLockStatusVO.unlocked(0);
        }
        RBucket<Long> lockBucket = redissonClient.getBucket(LOGIN_LOCK_USER_KEY + username.trim());
        Long lockAt = lockBucket.get();
        if (lockAt != null) {
            long ttlMs = lockBucket.remainTimeToLive();
            if (ttlMs > 0) {
                return LoginLockStatusVO.locked((ttlMs + 999) / 1000);
            }
        }
        RBucket<Integer> failBucket = redissonClient.getBucket(LOGIN_FAIL_USER_KEY + username.trim());
        Integer failCount = failBucket.get();
        return LoginLockStatusVO.unlocked(failCount == null ? 0 : failCount);
    }

    @Override
    public Map<String, LoginLockStatusVO> getUserLockStatusMap(Collection<String> usernames) {
        Map<String, LoginLockStatusVO> map = new HashMap<>();
        if (usernames == null || usernames.isEmpty()) {
            return map;
        }
        for (String username : usernames) {
            if (StringUtils.hasText(username)) {
                map.put(username.trim(), getUserLockStatus(username));
            }
        }
        return map;
    }

    @Override
    public LoginLockStatusVO getIpLockStatus(String ip) {
        if (!StringUtils.hasText(ip)) {
            return LoginLockStatusVO.unlocked(0);
        }
        RBucket<Long> lockBucket = redissonClient.getBucket(LOGIN_LOCK_IP_KEY + ip.trim());
        Long lockAt = lockBucket.get();
        if (lockAt != null) {
            long ttlMs = lockBucket.remainTimeToLive();
            if (ttlMs > 0) {
                return LoginLockStatusVO.locked((ttlMs + 999) / 1000);
            }
        }
        RBucket<Integer> failBucket = redissonClient.getBucket(LOGIN_FAIL_IP_KEY + ip.trim());
        Integer failCount = failBucket.get();
        return LoginLockStatusVO.unlocked(failCount == null ? 0 : failCount);
    }

    @Override
    public Map<String, LoginLockStatusVO> getIpLockStatusMap(Collection<String> ips) {
        Map<String, LoginLockStatusVO> map = new HashMap<>();
        if (ips == null || ips.isEmpty()) {
            return map;
        }
        for (String ip : ips) {
            if (StringUtils.hasText(ip)) {
                map.put(ip.trim(), getIpLockStatus(ip));
            }
        }
        return map;
    }

    @Override
    public void unlockUser(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        String key = username.trim();
        redissonClient.getBucket(LOGIN_FAIL_USER_KEY + key).delete();
        redissonClient.getBucket(LOGIN_LOCK_USER_KEY + key).delete();
    }

    @Override
    public void unlockIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return;
        }
        String key = ip.trim();
        redissonClient.getBucket(LOGIN_FAIL_IP_KEY + key).delete();
        redissonClient.getBucket(LOGIN_LOCK_IP_KEY + key).delete();
    }

    @Override
    public Set<String> listLockedUsernames() {
        return scanActiveLockSuffixes(LOGIN_LOCK_USER_KEY);
    }

    @Override
    public Set<String> listLockedIps() {
        return scanActiveLockSuffixes(LOGIN_LOCK_IP_KEY);
    }

    private Set<String> scanActiveLockSuffixes(String prefix) {
        Set<String> result = new HashSet<>();
        RKeys keys = redissonClient.getKeys();
        Iterable<String> keyNames = keys.getKeys(KeysScanOptions.defaults().pattern(prefix + "*"));
        for (String key : keyNames) {
            if (!StringUtils.hasText(key) || key.length() <= prefix.length()) {
                continue;
            }
            RBucket<Long> bucket = redissonClient.getBucket(key);
            Long lockAt = bucket.get();
            if (lockAt != null && bucket.remainTimeToLive() > 0) {
                result.add(key.substring(prefix.length()));
            }
        }
        return result;
    }

    private String getLockMessage(String lockKey, String lockType) {
        RBucket<Long> lockBucket = redissonClient.getBucket(lockKey);
        Long lockAt = lockBucket.get();
        if (lockAt == null) {
            return null;
        }
        long ttlSeconds = lockBucket.remainTimeToLive() / 1000;
        if (ttlSeconds <= 0) {
            return null;
        }
        long minutes = Math.max(1, (ttlSeconds + 59) / 60);
        return lockType + "已被临时锁定，请" + minutes + "分钟后再试";
    }

    private void increaseFailAndLock(String failKey, String lockKey, int maxRetry, long lockMinutes) {
        RBucket<Integer> failBucket = redissonClient.getBucket(failKey);
        Integer failCount = failBucket.get();
        int nextCount = (failCount == null ? 0 : failCount) + 1;
        setWithTtl(failBucket, nextCount, lockMinutes, TimeUnit.MINUTES);
        if (nextCount >= maxRetry) {
            setWithTtl(redissonClient.getBucket(lockKey), System.currentTimeMillis(), lockMinutes, TimeUnit.MINUTES);
            failBucket.delete();
        }
    }

    private <V> void setWithTtl(RBucket<V> bucket, V value, long duration, TimeUnit unit) {
        bucket.set(value, Duration.of(duration, unit.toChronoUnit()));
    }
}
