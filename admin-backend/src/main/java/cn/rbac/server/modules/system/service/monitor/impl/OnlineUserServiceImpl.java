package cn.rbac.server.modules.system.service.monitor.impl;

import cn.rbac.server.common.util.UserAgentUtils;
import cn.hutool.json.JSONUtil;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.common.util.ClientIpUtils;
import cn.rbac.server.modules.system.api.monitor.vo.OnlineUserVO;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.dept.DeptMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import lombok.Data;
import org.redisson.api.RBucket;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final String ONLINE_DETAIL_PREFIX = "monitor:online:detail:";
    private static final String ONLINE_ACTIVE_PREFIX = "dashboard:online:user:";

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private TokenService tokenService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;

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
        String ip = ClientIpUtils.resolve(request);
        String userAgent = request.getHeader("User-Agent");

        OnlineDetail detail = new OnlineDetail();
        detail.setUserId(userId);
        detail.setUsername(username);
        detail.setNickname(nickname);
        detail.setIpaddr(ip);
        detail.setLoginLocation(resolveLocation(ip));
        detail.setBrowser(UserAgentUtils.parseBrowser(userAgent));
        detail.setOs(UserAgentUtils.parseOs(request));
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
        bucket.set(JSONUtil.toJsonStr(detail), 1, TimeUnit.DAYS);

        redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId).set(now, 10, TimeUnit.MINUTES);
    }

    @Override
    public void touchLastAccess(Long userId) {
        long now = System.currentTimeMillis();
        redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId).set(now, 10, TimeUnit.MINUTES);

        RBucket<String> bucket = redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId);
        String json = bucket.get();
        if (json != null) {
            OnlineDetail detail = JSONUtil.toBean(json, OnlineDetail.class);
            detail.setLastAccessTime(now);
            bucket.set(JSONUtil.toJsonStr(detail), 1, TimeUnit.DAYS);
        }
    }

    @Override
    public List<OnlineUserVO> listOnlineUsers() {
        List<OnlineUserVO> list = new ArrayList<>();
        for (String token : tokenService.listActiveTokens()) {
            try {
                Long userId = tokenService.getUserId(token);
                if (userId == null) {
                    continue;
                }
                TokenService.LoginInfo loginInfo = tokenService.getLoginInfo(userId);
                if (loginInfo == null || !StringUtils.hasText(loginInfo.getToken())) {
                    continue;
                }
                RBucket<Long> active = redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId);
                Long lastActive = active.get();
                if (lastActive == null) {
                    continue;
                }

                OnlineUserVO vo = buildVo(userId, loginInfo);
                list.add(vo);
            } catch (Exception ignored) {
            }
        }
        list.sort((a, b) -> Long.compare(
                parseTime(b.getLastAccessTime()),
                parseTime(a.getLastAccessTime())));
        return list;
    }

    @Override
    public void forceLogout(Long userId) {
        tokenService.removeToken(userId);
        redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId).delete();
        redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId).delete();
    }

    private OnlineUserVO buildVo(Long userId, TokenService.LoginInfo loginInfo) {
        OnlineUserVO vo = new OnlineUserVO();
        vo.setUserId(userId);
        vo.setTokenId(String.valueOf(userId));
        vo.setTokenValue(loginInfo.getToken());

        OnlineDetail detail = loadDetail(userId);
        UserDO user = userMapper.selectById(userId);

        String loginName = detail != null && StringUtils.hasText(detail.getUsername())
                ? detail.getUsername()
                : (user != null ? user.getUsername() : loginInfo.getUsername());
        vo.setLoginName(loginName);

        String nickname = detail != null && StringUtils.hasText(detail.getNickname())
                ? detail.getNickname()
                : (user != null ? user.getNickname() : "");
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
        if (!StringUtils.hasText(ip)) {
            return "未知";
        }
        if ("127.0.0.1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "内网IP";
        }
        return "未知";
    }

    private String formatTime(Long millis) {
        if (millis == null) {
            return "";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
}
