package com.admin.server.modules.infra.service.monitor.impl;

import com.admin.server.common.util.UserAgentUtils;
import cn.hutool.json.JSONUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.admin.server.framework.config.DynamicConfigProvider;
import com.admin.server.framework.security.core.service.TokenService;
import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.common.util.IpLocationUtils;
import com.admin.server.modules.infra.api.monitor.vo.OnlineUserVO;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.infra.service.monitor.OnlineUserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final String ONLINE_DETAIL_PREFIX = "monitor:online:detail:";
    private static final String ONLINE_ACTIVE_PREFIX = "dashboard:online:user:";
    /** 同一用户两次 touch 间隔，减轻每个 API 请求写 Redis 的压力 */
    private static final long TOUCH_THROTTLE_MS = 45_000L;

    /** 进程内节流（单机部署）；登录/强退时同步维护 */
    private final ConcurrentHashMap<Long, Long> touchThrottleCache = new ConcurrentHashMap<>();

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private TokenService tokenService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private DynamicConfigProvider dynamicConfigProvider;

    @Data
    public static class OnlineDetail {
        private Long userId;
        private String username;
        private String nickname;
        private String deptName;
        private String ipaddr;
        private String loginLocation;
        private String browser;
        private String os;
        private Long loginTime;
        private Long lastAccessTime;
    }

    @Override
    public void recordLoginSession(Long userId, String username, String nickname, HttpServletRequest request) {
        recordLoginSession(userId, username, nickname, ClientIpUtils.resolve(request), request.getHeader("User-Agent"));
    }

    @Override
    public void recordLoginSession(Long userId, String username, String nickname, String clientIp, String userAgent) {
        OnlineDetail detail = new OnlineDetail();
        detail.setUserId(userId);
        detail.setUsername(username);
        detail.setNickname(nickname);
        detail.setIpaddr(clientIp);
        detail.setLoginLocation(resolveLocation(clientIp));
        detail.setBrowser(UserAgentUtils.parseBrowser(userAgent));
        detail.setOs(UserAgentUtils.parseOsFromUserAgent(userAgent));
        long now = System.currentTimeMillis();
        detail.setLoginTime(now);
        detail.setLastAccessTime(now);

        UserDO user = userMapper.selectById(userId);
        if (user != null && user.getDeptId() != null) {
            DeptDO dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                detail.setDeptName(dept.getName());
            }
        }

        RBucket<String> bucket = redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId);
        setWithTtl(bucket, JSONUtil.toJsonStr(detail), 1, TimeUnit.DAYS);

        refreshActiveMarker(userId, now);
        touchThrottleCache.put(userId, now);
    }

    @Override
    public void touchLastAccess(Long userId) {
        if (userId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long lastTouch = touchThrottleCache.get(userId);
        if (lastTouch != null && now - lastTouch < TOUCH_THROTTLE_MS) {
            return;
        }
        touchThrottleCache.put(userId, now);
        refreshActiveMarker(userId, now);

        RBucket<String> bucket = redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId);
        String json = bucket.get();
        if (json != null) {
            OnlineDetail detail = JSONUtil.toBean(json, OnlineDetail.class);
            detail.setLastAccessTime(now);
            setWithTtl(bucket, JSONUtil.toJsonStr(detail), 1, TimeUnit.DAYS);
        }
    }

    @Override
    public int countOnlineUsers() {
        return resolveActiveUserIds().size();
    }

    @Override
    public List<OnlineUserVO> listOnlineUsers() {
        List<OnlineUserVO> list = new ArrayList<>();
        for (Long userId : resolveActiveUserIds()) {
            try {
                if (!isUserActive(userId)) {
                    continue;
                }
                TokenService.LoginInfo loginInfo = tokenService.getLoginInfo(userId);
                if (loginInfo == null) {
                    continue;
                }
                if (redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId).get() == null) {
                    touchLastAccess(userId);
                }
                list.add(buildVo(userId, loginInfo));
            } catch (Exception e) {
                log.warn("加载在线用户信息失败 userId={}", userId, e);
            }
        }
        list.sort((a, b) -> Long.compare(
                parseTime(b.getLastAccessTime()),
                parseTime(a.getLastAccessTime())));
        return list;
    }

    private boolean isUserActive(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            return StpUtil.isLogin(userId);
        } catch (Exception e) {
            return tokenService.getLoginInfo(userId) != null;
        }
    }

    private List<Long> resolveActiveUserIds() {
        Set<Long> ids = new LinkedHashSet<>(tokenService.listActiveUserIds());
        if (ids.isEmpty()) {
            collectUserIdsFromRedisSessions(ids);
        }
        List<Long> result = new ArrayList<>();
        for (Long userId : ids) {
            if (isUserActive(userId)) {
                result.add(userId);
            }
        }
        return result;
    }

    /** 从 Sa-Token 在 Redis 中的 session 键解析在线用户（searchSessionId 为空时的兜底） */
    @SuppressWarnings("deprecation")
    private void collectUserIdsFromRedisSessions(Set<Long> ids) {
        try {
            RKeys keys = redissonClient.getKeys();
            for (String pattern : new String[] {
                    "Authorization:login:session:*",
                    "Authorization:login:token:*"
            }) {
                Iterable<String> found = keys.getKeysByPattern(pattern, 500);
                for (String key : found) {
                    Long userId = parseLoginIdFromSaTokenKey(key);
                    if (userId != null && isUserActive(userId)) {
                        ids.add(userId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从 Redis 会话键解析在线用户失败", e);
        }
    }

    private Long parseLoginIdFromSaTokenKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        int idx = key.lastIndexOf(':');
        if (idx < 0 || idx >= key.length() - 1) {
            return null;
        }
        try {
            String tokenOrId = key.substring(idx + 1).trim();
            if (tokenOrId.matches("^\\d+$")) {
                return Long.parseLong(tokenOrId);
            }
            Object loginId = StpUtil.getLoginIdByToken(tokenOrId);
            if (loginId != null) {
                return Long.parseLong(loginId.toString());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void forceLogout(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            StpUtil.kickout(userId);
        } catch (Exception ignored) {}
        try {
            StpUtil.logout(userId);
        } catch (Exception ignored) {}
        tokenService.removeToken(userId);
        redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId).delete();
        redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId).delete();
        touchThrottleCache.remove(userId);
    }

    private OnlineUserVO buildVo(Long userId, TokenService.LoginInfo loginInfo) {
        OnlineUserVO vo = new OnlineUserVO();
        vo.setUserId(userId);
        vo.setTokenId(String.valueOf(userId));

        OnlineDetail detail = loadDetail(userId);
        UserDO user = userMapper.selectById(userId);

        String loginName = detail != null && StringUtils.hasText(detail.getUsername())
                ? detail.getUsername()
                : (user != null ? user.getUsername() : loginInfo.getUsername());
        vo.setLoginName(loginName);

        String nickname = detail != null && StringUtils.hasText(detail.getNickname())
                ? detail.getNickname()
                : (user != null ? user.getNickname() : "");
        vo.setNickname(nickname);
        if (user != null && StringUtils.hasText(user.getAvatar())) {
            vo.setAvatar(user.getAvatar());
        }
        String deptName = detail != null && StringUtils.hasText(detail.getDeptName()) ? detail.getDeptName() : "";
        vo.setDeptName(StringUtils.hasText(deptName) ? deptName : nickname);

        if (detail != null) {
            vo.setIpaddr(detail.getIpaddr());
            vo.setLoginLocation(detail.getLoginLocation());
            vo.setBrowser(detail.getBrowser());
            vo.setOs(detail.getOs());
            vo.setLoginTime(formatTime(detail.getLoginTime() != null ? detail.getLoginTime() : loginInfo.getCreateTime()));
            vo.setLastAccessTime(formatTime(detail.getLastAccessTime() != null ? detail.getLastAccessTime() : detail.getLoginTime()));
        } else {
            vo.setIpaddr("");
            vo.setLoginLocation("");
            vo.setBrowser("");
            vo.setOs("");
            vo.setLoginTime(formatTime(loginInfo.getCreateTime()));
            vo.setLastAccessTime(formatTime(loginInfo.getCreateTime()));
        }
        vo.setStatus(1);
        return vo;
    }

    private OnlineDetail loadDetail(Long userId) {
        RBucket<String> detailBucket = redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId);
        String json = detailBucket.get();
        if (json == null) {
            return null;
        }
        try {
            return JSONUtil.toBean(json, OnlineDetail.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveLocation(String ip) {
        return IpLocationUtils.resolve(ip);
    }

    private String formatTime(Long millis) {
        if (millis == null) {
            return "";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void refreshActiveMarker(Long userId, long timestamp) {
        long minutes = Math.max(10L, dynamicConfigProvider.getTokenExpirationMs() / 60_000L);
        setWithTtl(redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId), timestamp, minutes, TimeUnit.MINUTES);
    }

    private long parseTime(String time) {
        if (!StringUtils.hasText(time)) {
            return 0L;
        }
        try {
            return LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    @SuppressWarnings("deprecation")
    private <V> void setWithTtl(RBucket<V> bucket, V value, long duration, TimeUnit unit) {
        bucket.set(value, duration, unit);
    }
}
