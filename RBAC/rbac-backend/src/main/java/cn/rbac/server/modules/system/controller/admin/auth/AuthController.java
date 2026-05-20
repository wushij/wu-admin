package cn.rbac.server.modules.system.controller.admin.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.rbac.server.framework.web.UserAgentUtils;
import cn.rbac.server.framework.web.ClientIpUtils;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.RAtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Resource
    private UserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private TokenService tokenService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private OnlineUserService onlineUserService;

    @Value("${auth.security.captcha-enabled:true}")
    private boolean captchaEnabled;

    /** 单 IP 每分钟最多拉取验证码次数，防刷图 */
    @Value("${auth.security.captcha-per-ip-minute:40}")
    private int captchaPerIpMinute;

    /** 单 IP 每分钟最多登录请求次数 */
    @Value("${auth.security.login-per-ip-minute:30}")
    private int loginPerIpMinute;

    /** 单 IP 每分钟最多注册次数 */
    @Value("${auth.security.register-per-ip-minute:10}")
    private int registerPerIpMinute;
    
    private static final String CAPTCHA_KEY = "captcha:";
    private static final String ONLINE_USER_KEY = "dashboard:online:user:";
    private static final String LOGIN_FAIL_USER_KEY = "auth:login:fail:user:";
    private static final String LOGIN_FAIL_IP_KEY = "auth:login:fail:ip:";
    private static final String LOGIN_LOCK_USER_KEY = "auth:login:lock:user:";
    private static final String LOGIN_LOCK_IP_KEY = "auth:login:lock:ip:";
    private static final int LOGIN_MAX_FAIL_COUNT = 5;
    private static final long LOGIN_LOCK_MINUTES = 10;
    
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public CommonResult<Map<String, Object>> captcha(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String rlMsg = rateLimitByIp(clientIp, "captcha", captchaPerIpMinute);
        if (rlMsg != null) {
            return CommonResult.error(429, rlMsg);
        }

        String uuid = IdUtil.simpleUUID();
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 50);
        String code = lineCaptcha.getCode();
        String imageBase64 = lineCaptcha.getImageBase64();
        
        // 存储到Redis，5分钟过期（使用RedissonClient）
        RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY + uuid);
        bucket.set(code.toLowerCase(), 5, TimeUnit.MINUTES);
        
        Map<String, Object> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("img", imageBase64);
        return CommonResult.success(result);
    }
    
    @Operation(summary = "获取登录配置")
    @GetMapping("/config")
    public CommonResult<Map<String, Object>> config() {
        Map<String, Object> result = new HashMap<>();
        
        // 登录配置
        Map<String, Object> loginConfig = new HashMap<>();
        loginConfig.put("captchaEnabled", captchaEnabled);
        loginConfig.put("rememberMe", true);
        result.put("login", loginConfig);
        
        // 注册配置
        Map<String, Object> registerConfig = new HashMap<>();
        registerConfig.put("enabled", true);
        registerConfig.put("verifyEmail", false);
        registerConfig.put("verifyPhone", false);
        result.put("register", registerConfig);
        
        return CommonResult.success(result);
    }
    
    @Operation(summary = "登录")
    @PostMapping("/login")
    public CommonResult<Map<String, Object>> login(@RequestBody LoginReqVO reqVO, HttpServletRequest request) {
        String username = reqVO.getUsername() == null ? "" : reqVO.getUsername().trim();
        String clientIp = getClientIp(request);

        String rlMsg = rateLimitByIp(clientIp, "login", loginPerIpMinute);
        if (rlMsg != null) {
            recordLoginLog(username, 1, rlMsg, request);
            return CommonResult.error(429, rlMsg);
        }

        String lockMessage = checkLoginLock(username, clientIp);
        if (lockMessage != null) {
            recordLoginLog(username, 1, lockMessage, request);
            return CommonResult.error(429, lockMessage);
        }

        String captchaUuid = reqVO.getUuid() == null ? "" : reqVO.getUuid().trim();
        String captchaInput = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
        if (captchaEnabled) {
            if (captchaUuid.isEmpty() || captchaInput.isEmpty()) {
                handleLoginFailure(username, clientIp);
                recordLoginLog(username, 1, "未提供验证码", request);
                return CommonResult.error(400, "请先完成图形验证码");
            }
            RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY + captchaUuid);
            String captchaCode = bucket.get();
            if (captchaCode == null || !captchaCode.equals(captchaInput.toLowerCase())) {
                handleLoginFailure(username, clientIp);
                recordLoginLog(username, 1, "验证码错误", request);
                return CommonResult.error(400, "验证码错误或已过期");
            }
            bucket.delete();
        }
        
        // 查询用户
        UserDO user = userMapper.selectOne(
            new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username)
        );
        if (user == null) {
            handleLoginFailure(username, clientIp);
            recordLoginLog(username, 1, "用户不存在", request);
            return CommonResult.error(401, "用户不存在");
        }
        // 校验密码
        if (!passwordEncoder.matches(reqVO.getPassword(), user.getPassword())) {
            handleLoginFailure(username, clientIp);
            recordLoginLog(username, 1, "密码错误", request);
            return CommonResult.error(401, "密码错误");
        }

        clearLoginFailure(username, clientIp);

        // 生成token并存储到Redis
        String token = tokenService.createToken(user.getId(), user.getUsername());
        
        onlineUserService.recordLoginSession(user.getId(), user.getUsername(), user.getNickname(), request);

        // 记录登录成功日志
        recordLoginLog(username, 0, "登录成功", request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        return CommonResult.success(result);
    }
    
    /**
     * 记录登录日志
     */
    private void recordLoginLog(String username, Integer status, String msg, HttpServletRequest request) {
        LoginLogDO log = new LoginLogDO();
        log.setUsername(username);
        log.setStatus(status);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        
        // 获取IP地址
        String ip = getClientIp(request);
        log.setIpaddr(ip);
        
        // 解析IP地址获取地理位置（简单实现）
        log.setLoginLocation(getLocationByIP(ip));
        
        String userAgentStr = request.getHeader("User-Agent");
        log.setBrowser(UserAgentUtils.parseBrowser(userAgentStr));
        log.setOs(UserAgentUtils.parseOs(request));
        
        loginLogMapper.insert(log);
    }

    /**
     * 按 IP 固定窗口（每分钟）限流，用于反爬、防刷接口。
     *
     * @return 超限时的提示文案；null 表示放行
     */
    private String rateLimitByIp(String clientIp, String action, int maxPerMinute) {
        if (maxPerMinute <= 0) {
            return null;
        }
        long minute = System.currentTimeMillis() / 60_000L;
        String redisKey = "auth:rl:" + action + ":" + clientIp + ":" + minute;
        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);
        long n = counter.incrementAndGet();
        if (n == 1) {
            counter.expire(90, TimeUnit.SECONDS);
        }
        if (n > maxPerMinute) {
            return "请求过于频繁，请稍后再试";
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        return ClientIpUtils.resolve(request);
    }

    private String checkLoginLock(String username, String ip) {
        String userLockMsg = getLockMessage(LOGIN_LOCK_USER_KEY + username, "账号");
        if (userLockMsg != null) {
            return userLockMsg;
        }
        return getLockMessage(LOGIN_LOCK_IP_KEY + ip, "IP");
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

    private void handleLoginFailure(String username, String ip) {
        increaseFailAndLock(LOGIN_FAIL_USER_KEY + username, LOGIN_LOCK_USER_KEY + username);
        increaseFailAndLock(LOGIN_FAIL_IP_KEY + ip, LOGIN_LOCK_IP_KEY + ip);
    }

    private void increaseFailAndLock(String failKey, String lockKey) {
        RBucket<Integer> failBucket = redissonClient.getBucket(failKey);
        Integer failCount = failBucket.get();
        int nextCount = (failCount == null ? 0 : failCount) + 1;
        failBucket.set(nextCount, LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
        if (nextCount >= LOGIN_MAX_FAIL_COUNT) {
            redissonClient.getBucket(lockKey).set(System.currentTimeMillis(), LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
            failBucket.delete();
        }
    }

    private void clearLoginFailure(String username, String ip) {
        redissonClient.getBucket(LOGIN_FAIL_USER_KEY + username).delete();
        redissonClient.getBucket(LOGIN_FAIL_IP_KEY + ip).delete();
        redissonClient.getBucket(LOGIN_LOCK_USER_KEY + username).delete();
        redissonClient.getBucket(LOGIN_LOCK_IP_KEY + ip).delete();
    }

    
    /**
     * 根据IP地址获取地理位置
     * 使用 IP2Region 库进行IP地址解析
     */
    private String getLocationByIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "未知";
        }
        
        // 内网IP
        if (ip.startsWith("192.168.") || ip.startsWith("10.") || 
            ip.startsWith("172.16.") || ip.startsWith("127.") ||
            ip.startsWith("172.17.") || ip.startsWith("172.18.") ||
            ip.startsWith("172.19.") || ip.startsWith("172.20.") ||
            ip.startsWith("172.21.") || ip.startsWith("172.22.") ||
            ip.startsWith("172.23.") || ip.startsWith("172.24.") ||
            ip.startsWith("172.25.") || ip.startsWith("172.26.") ||
            ip.startsWith("172.27.") || ip.startsWith("172.28.") ||
            ip.startsWith("172.29.") || ip.startsWith("172.30.") ||
            ip.startsWith("172.31.")) {
            return "内网IP";
        }
        
        try {
            // 从 classpath 加载 IP2Region 数据库文件
            ClassPathResource resource = new ClassPathResource("ip2region/ip2region.xdb");
            if (!resource.exists()) {
                log.warn("IP2Region数据库文件不存在: {}", resource.getPath());
                return "未知";
            }
            
            InputStream is = resource.getInputStream();
            byte[] dbBuff = new byte[is.available()];
            is.read(dbBuff);
            is.close();
            
            // 使用字节数组创建Searcher
            Searcher searcher = Searcher.newWithBuffer(dbBuff);
            String region = searcher.search(ip);
            searcher.close();
            
            if (region != null && !region.isEmpty()) {
                // 格式化输出：中国|0|江苏省|苏州市|电信 -> 江苏苏州
                String[] parts = region.split("\\|");
                StringBuilder location = new StringBuilder();
                
                // 跳过国家和区域字段，取省份和城市
                if (parts.length >= 3 && !"0".equals(parts[2])) {
                    location.append(parts[2].replace("省", "").replace("自治区", ""));
                }
                if (parts.length >= 4 && !"0".equals(parts[3])) {
                    location.append(parts[3].replace("市", ""));
                }
                
                // 添加运营商信息
                if (parts.length >= 5 && !"0".equals(parts[4])) {
                    location.append("(").append(parts[4]).append(")");
                }
                
                return location.length() > 0 ? location.toString() : "未知";
            }
        } catch (Exception e) {
            log.warn("IP地址解析失败: {}, 错误: {}", ip, e.getMessage());
        }
        
        return "未知";
    }
    
    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public CommonResult<Map<String, Object>> info(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = tokenService.getUserId(token);
        UserDO user = userMapper.selectById(userId);
        
        onlineUserService.touchLastAccess(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        // 获取用户权限
        result.put("roles", permissionService.getUserRoleIdListByUserId(userId));
        result.put("menus", permissionService.getUserMenuList(userId));
        return CommonResult.success(result);
    }
    
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public CommonResult<Boolean> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return CommonResult.success(true);
        }
        String token = authHeader.replace("Bearer ", "").trim();
        Long userId = tokenService.getUserId(token);
        if (userId != null) {
            tokenService.removeToken(userId);
            onlineUserService.forceLogout(userId);
        }
        return CommonResult.success(true);
    }
    
    @Operation(summary = "注册")
    @PostMapping("/register")
    public CommonResult<Boolean> register(@RequestBody RegisterReqVO reqVO, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String rlMsg = rateLimitByIp(clientIp, "register", registerPerIpMinute);
        if (rlMsg != null) {
            return CommonResult.error(429, rlMsg);
        }

        String regCaptchaUuid = reqVO.getUuid() == null ? "" : reqVO.getUuid().trim();
        String regCaptchaCode = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
        if (captchaEnabled) {
            if (regCaptchaUuid.isEmpty() || regCaptchaCode.isEmpty()) {
                return CommonResult.error(400, "请先完成图形验证码");
            }
            RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY + regCaptchaUuid);
            String captchaCode = bucket.get();
            if (captchaCode == null || !captchaCode.equals(regCaptchaCode.toLowerCase())) {
                return CommonResult.error(400, "验证码错误或已过期");
            }
            bucket.delete();
        }
        
        // 检查用户名是否已存在
        UserDO existUser = userMapper.selectOne(
            new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, reqVO.getUsername())
        );
        if (existUser != null) {
            return CommonResult.error(400, "用户名已存在");
        }
        
        // 创建用户
        UserDO user = new UserDO();
        user.setUsername(reqVO.getUsername());
        user.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        user.setNickname(reqVO.getNickname() != null ? reqVO.getNickname() : reqVO.getUsername());
        user.setStatus(1); // 默认启用
        userMapper.insert(user);
        
        // 分配默认普通用户角色
        RoleDO defaultRole = roleMapper.selectOne(
            new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getCode, "user")
        );
        if (defaultRole != null) {
            Set<Long> roleIds = new HashSet<>();
            roleIds.add(defaultRole.getId());
            permissionService.assignUserRole(user.getId(), roleIds);
        }
        
        return CommonResult.success(true);
    }
    
    @Data
    public static class LoginReqVO {
        private String username;
        private String password;
        private String uuid;
        private String code;
    }
    
    @Data
    public static class RegisterReqVO {
        private String username;
        private String password;
        private String nickname;
        private String uuid;
        private String code;
    }
}