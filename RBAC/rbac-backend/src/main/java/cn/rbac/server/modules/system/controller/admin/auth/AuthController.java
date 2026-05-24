package cn.rbac.server.modules.system.controller.admin.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public CommonResult<Map<String, Object>> captcha() {
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
        loginConfig.put("captchaEnabled", true);
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

        String lockMessage = checkLoginLock(username, clientIp);
        if (lockMessage != null) {
            recordLoginLog(username, 1, lockMessage, request);
            return CommonResult.error(429, lockMessage);
        }

        // 校验验证码
        if (reqVO.getUuid() != null && reqVO.getCode() != null) {
            RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY + reqVO.getUuid());
            String captchaCode = bucket.get();
            if (captchaCode == null || !captchaCode.equals(reqVO.getCode().toLowerCase())) {
                handleLoginFailure(username, clientIp);
                recordLoginLog(username, 1, "验证码错误", request);
                return CommonResult.error(400, "验证码错误或已过期");
            }
            // 验证成功后删除验证码
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
        
        // 记录用户在线状态
        RBucket<Long> onlineBucket = redissonClient.getBucket(ONLINE_USER_KEY + user.getId());
        onlineBucket.set(System.currentTimeMillis(), 10, TimeUnit.MINUTES);
        
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
        
        // 获取浏览器和操作系统信息
        String userAgentStr = request.getHeader("User-Agent");
        if (userAgentStr != null && !userAgentStr.isEmpty()) {
            log.setBrowser(parseBrowser(userAgentStr));
            log.setOs(parseOs(userAgentStr));
        } else {
            log.setBrowser("Unknown");
            log.setOs("Unknown");
        }
        
        loginLogMapper.insert(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "unknown" : ip;
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

    private String parseBrowser(String userAgentStr) {
        Matcher edgeMatcher = Pattern.compile("Edg/([\\d.]+)").matcher(userAgentStr);
        if (edgeMatcher.find()) {
            return "MSEdge " + majorVersion(edgeMatcher.group(1));
        }
        Matcher chromeMatcher = Pattern.compile("Chrome/([\\d.]+)").matcher(userAgentStr);
        if (chromeMatcher.find() && !userAgentStr.contains("Edg/")) {
            return "Chrome " + majorVersion(chromeMatcher.group(1));
        }
        Matcher firefoxMatcher = Pattern.compile("Firefox/([\\d.]+)").matcher(userAgentStr);
        if (firefoxMatcher.find()) {
            return "Firefox " + majorVersion(firefoxMatcher.group(1));
        }
        Matcher safariMatcher = Pattern.compile("Version/([\\d.]+).*Safari").matcher(userAgentStr);
        if (safariMatcher.find() && !userAgentStr.contains("Chrome/") && !userAgentStr.contains("Edg/")) {
            return "Safari " + majorVersion(safariMatcher.group(1));
        }
        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        String fallbackName = userAgent.getBrowser().getName();
        return (fallbackName == null || fallbackName.isEmpty()) ? "Unknown" : fallbackName;
    }

    private String parseOs(String userAgentStr) {
        if (userAgentStr.contains("Windows NT 10.0")) {
            if (userAgentStr.contains("Windows 11") || userAgentStr.contains("Win11")) {
                return "Windows 11";
            }
            return "Windows 10";
        }
        if (userAgentStr.contains("Windows NT 6.3")) return "Windows 8.1";
        if (userAgentStr.contains("Windows NT 6.2")) return "Windows 8";
        if (userAgentStr.contains("Windows NT 6.1")) return "Windows 7";
        if (userAgentStr.contains("Windows NT 6.0")) return "Windows Vista";
        if (userAgentStr.contains("Windows NT 5.1")) return "Windows XP";

        Matcher androidMatcher = Pattern.compile("Android ([\\d.]+)").matcher(userAgentStr);
        if (androidMatcher.find()) return "Android " + androidMatcher.group(1);

        Matcher iosMatcher = Pattern.compile("(?:iPhone|CPU (?:iPhone )?OS) ([\\d_]+)").matcher(userAgentStr);
        if (iosMatcher.find()) return "iOS " + iosMatcher.group(1).replace("_", ".");

        Matcher macMatcher = Pattern.compile("Mac OS X ([\\d_]+)").matcher(userAgentStr);
        if (macMatcher.find()) return "macOS " + macMatcher.group(1).replace("_", ".");

        if (userAgentStr.contains("Ubuntu")) return "Ubuntu";
        if (userAgentStr.contains("CentOS")) return "CentOS";
        if (userAgentStr.contains("Fedora")) return "Fedora";
        if (userAgentStr.contains("Debian")) return "Debian";
        if (userAgentStr.contains("Linux")) return "Linux";

        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        String fallbackOs = userAgent.getOs().getName();
        return (fallbackOs == null || fallbackOs.isEmpty()) ? "Unknown" : fallbackOs;
    }

    private String majorVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "";
        }
        String[] parts = version.split("\\.");
        return parts.length > 0 ? parts[0] : version;
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
        
        // 更新用户活跃状态
        RBucket<Long> onlineBucket = redissonClient.getBucket(ONLINE_USER_KEY + userId);
        onlineBucket.set(System.currentTimeMillis(), 10, TimeUnit.MINUTES);
        
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
    public CommonResult<Boolean> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = tokenService.getUserId(token);
        tokenService.removeToken(userId);
        
        // 清除用户在线状态
        RBucket<Long> onlineBucket = redissonClient.getBucket(ONLINE_USER_KEY + userId);
        onlineBucket.delete();
        
        return CommonResult.success(true);
    }
    
    @Operation(summary = "注册")
    @PostMapping("/register")
    public CommonResult<Boolean> register(@RequestBody RegisterReqVO reqVO) {
        // 校验验证码（如果提供了）
        if (reqVO.getUuid() != null && reqVO.getCode() != null) {
            RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY + reqVO.getUuid());
            String captchaCode = bucket.get();
            if (captchaCode == null || !captchaCode.equals(reqVO.getCode().toLowerCase())) {
                return CommonResult.error(400, "验证码错误或已过期");
            }
            // 验证成功后删除验证码
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