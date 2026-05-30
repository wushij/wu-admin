package cn.rbac.server.modules.system.api.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.rbac.server.common.util.UserAgentUtils;
import cn.rbac.server.common.util.ClientIpUtils;
import cn.rbac.server.common.pojo.CommonResult;
import cn.dev33.satoken.stp.StpUtil;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.service.loginlog.LoginLogService;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.approval.RegisterApprovalService;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.rbac.server.modules.system.sms.AliyunDypnsSmsVerifyService;
import cn.rbac.server.modules.system.sms.SmsServiceFactory;
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
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.time.LocalDate;
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
    private LoginLogService loginLogService;
    @Resource
    private OnlineUserService onlineUserService;
    @Resource
    private SystemConfigHelper systemConfigHelper;
    @Resource
    private RegisterApprovalService registerApprovalService;
    @Resource
    private SmsServiceFactory smsServiceFactory;
    @Resource
    private AliyunDypnsSmsVerifyService aliyunDypnsSmsVerifyService;

    
    private static final String CAPTCHA_KEY = "captcha:";
    private static final String SMS_CODE_KEY = "sms:login:";
    private static final String SMS_LIMIT_KEY = "sms:limit:";
    private static final String SMS_DAILY_PHONE_KEY = "sms:daily:phone:";
    private static final String SMS_DAILY_IP_KEY = "sms:daily:ip:";
    private static final String ONLINE_USER_KEY = "dashboard:online:user:";
    private static final String LOGIN_FAIL_USER_KEY = "auth:login:fail:user:";
    private static final String LOGIN_FAIL_IP_KEY = "auth:login:fail:ip:";
    private static final String LOGIN_LOCK_USER_KEY = "auth:login:lock:user:";
    private static final String LOGIN_LOCK_IP_KEY = "auth:login:lock:ip:";
    
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public CommonResult<Map<String, Object>> captcha(
            @RequestParam(value = "scene", defaultValue = "login") String scene,
            HttpServletRequest request) {
        boolean captchaEnabled = "register".equalsIgnoreCase(scene)
                ? systemConfigHelper.isRegisterCaptchaEnabled()
                : systemConfigHelper.isCaptchaEnabled();
        if (!captchaEnabled) {
            return CommonResult.error(400, "当前未启用验证码");
        }
        String captchaType = "register".equalsIgnoreCase(scene)
                ? systemConfigHelper.getRegisterCaptchaType()
                : systemConfigHelper.getCaptchaType();
        if (SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(captchaType)) {
            return CommonResult.error(400, "当前为滑块验证码，无需拉取图片验证码");
        }

        String clientIp = getClientIp(request);
        String rlMsg = rateLimitByIp(clientIp, "captcha", systemConfigHelper.getCaptchaPerIpMinute());
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
        return CommonResult.success(systemConfigHelper.buildPublicConfig());
    }
    
    @Operation(summary = "登录")
    @PostMapping("/login")
    public CommonResult<Map<String, Object>> login(@RequestBody LoginReqVO reqVO, HttpServletRequest request) {
        if (SystemConfigHelper.LOGIN_TYPE_SMS.equalsIgnoreCase(reqVO.getLoginType())) {
            return loginBySms(reqVO, request);
        }
        return loginByAccount(reqVO, request);
    }

    private CommonResult<Map<String, Object>> loginByAccount(LoginReqVO reqVO, HttpServletRequest request) {
        String username = reqVO.getUsername() == null ? "" : reqVO.getUsername().trim();
        String clientIp = getClientIp(request);

        String rlMsg = rateLimitByIp(clientIp, "login", systemConfigHelper.getLoginPerIpMinute());
        if (rlMsg != null) {
            recordLoginLog(null, username, 1, rlMsg, request);
            return CommonResult.error(429, rlMsg);
        }

        String lockMessage = checkLoginLock(username, clientIp);
        if (lockMessage != null) {
            recordLoginLog(null, username, 1, lockMessage, request);
            return CommonResult.error(429, lockMessage);
        }

        String captchaErr = validateLoginCaptcha(reqVO);
        if (captchaErr != null) {
            handleLoginFailure(username, clientIp);
            recordLoginLog(null, username, 1, captchaErr, request);
            return CommonResult.error(400, captchaErr);
        }

        UserDO user = userMapper.selectOne(
            new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username)
        );
        if (user == null) {
            handleLoginFailure(username, clientIp);
            recordLoginLog(null, username, 1, "用户不存在", request);
            return CommonResult.error(400, "账号或密码错误");
        }
        if (!passwordEncoder.matches(reqVO.getPassword(), user.getPassword())) {
            handleLoginFailure(username, clientIp);
            recordLoginLog(user.getId(), username, 1, "密码错误", request);
            return CommonResult.error(400, "账号或密码错误");
        }

        String statusErr = checkUserLoginStatus(user);
        if (statusErr != null) {
            recordLoginLog(user.getId(), username, 1, statusErr, request);
            return CommonResult.error(403, statusErr);
        }

        return completeLogin(user, request);
    }

    private CommonResult<Map<String, Object>> loginBySms(LoginReqVO reqVO, HttpServletRequest request) {
        if (!systemConfigHelper.isSmsLoginEnabled()) {
            return CommonResult.error(400, "当前未启用短信登录");
        }
        if (!systemConfigHelper.isSmsEnabled()) {
            return CommonResult.error(400, "短信功能未启用");
        }

        String phone = reqVO.getPhone() == null ? "" : reqVO.getPhone().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return CommonResult.error(400, "请输入正确的手机号");
        }

        UserDO user = findUserByMobile(phone);
        String username = user != null ? user.getUsername() : phone;
        String clientIp = getClientIp(request);

        String rlMsg = rateLimitByIp(clientIp, "login", systemConfigHelper.getLoginPerIpMinute());
        if (rlMsg != null) {
            recordLoginLog(null, username, 1, rlMsg, request);
            return CommonResult.error(429, rlMsg);
        }

        String lockMessage = checkLoginLock(username, clientIp);
        if (lockMessage != null) {
            recordLoginLog(null, username, 1, lockMessage, request);
            return CommonResult.error(429, lockMessage);
        }

        String captchaErr = validateSmsLoginCaptcha(reqVO);
        if (captchaErr != null) {
            handleLoginFailure(username, clientIp);
            recordLoginLog(null, username, 1, captchaErr, request);
            return CommonResult.error(400, captchaErr);
        }

        if (user == null) {
            handleLoginFailure(username, clientIp);
            recordLoginLog(null, username, 1, "该手机号未绑定任何账号", request);
            return CommonResult.error(400, "该手机号未绑定任何账号");
        }

        String statusErr = checkUserLoginStatus(user);
        if (statusErr != null) {
            recordLoginLog(user.getId(), username, 1, statusErr, request);
            return CommonResult.error(403, statusErr);
        }

        return completeLogin(user, request);
    }

    private CommonResult<Map<String, Object>> completeLogin(UserDO user, HttpServletRequest request) {
        String username = user.getUsername();
        String clientIp = getClientIp(request);
        clearLoginFailure(username, clientIp);

        String token = tokenService.createToken(user.getId(), user.getUsername());
        StpUtil.getSession().set(TokenService.SESSION_NICKNAME, user.getNickname());

        onlineUserService.recordLoginSession(user.getId(), user.getUsername(), user.getNickname(), request);
        recordLoginLog(user.getId(), username, 0, "登录成功", request);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        return CommonResult.success(result);
    }
    
    /**
     * 记录登录日志（写入 userId，供个人中心按用户查询）
     */
    private void recordLoginLog(Long userId, String username, Integer status, String msg, HttpServletRequest request) {
        LoginLogDO log = new LoginLogDO();
        if (userId == null && StringUtils.hasText(username)) {
            UserDO u = userMapper.selectOne(
                    new LambdaQueryWrapper<UserDO>()
                            .eq(UserDO::getUsername, username)
                            .select(UserDO::getId));
            if (u != null) {
                userId = u.getId();
            }
        }
        log.setUserId(userId);
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
        
        loginLogService.recordAsync(log);
    }

    /**
     * 按 IP 固定窗口（每分钟）限流，用于反爬、防刷接口。
     *
     * @return 超限时的提示文案；null 表示放行
     */
    private String validateLoginCaptcha(LoginReqVO reqVO) {
        if (!systemConfigHelper.isCaptchaEnabled()) {
            return null;
        }
        String captchaType = systemConfigHelper.getCaptchaType();
        String captchaInput = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
        if (SystemConfigHelper.CAPTCHA_TYPE_SMS.equals(captchaType)) {
            return null;
        }
        if (SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(captchaType)) {
            if (!SystemConfigHelper.SLIDER_VERIFIED_CODE.equals(captchaInput)) {
                return "请完成滑块验证";
            }
            return null;
        }
        return validateImageCaptcha(reqVO.getUuid(), captchaInput);
    }

    private String validateSmsLoginCaptcha(LoginReqVO reqVO) {
        if (!systemConfigHelper.isSmsEnabled()) {
            return "短信功能未启用";
        }
        String phone = reqVO.getPhone() == null ? "" : reqVO.getPhone().trim();
        String captchaInput = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return "请输入正确的手机号";
        }
        if (captchaInput.isEmpty()) {
            return "请输入短信验证码";
        }
        if (findUserByMobile(phone) == null) {
            return "该手机号未绑定任何账号";
        }
        RBucket<String> bucket = redissonClient.getBucket(SMS_CODE_KEY + phone);
        String cached = bucket.get();
        if (cached != null && cached.equalsIgnoreCase(captchaInput)) {
            bucket.delete();
            return null;
        }
        // 模板使用 ##code## 时走阿里云云端核验；自定义 code 以 Redis 为准
        if (systemConfigHelper.isAliyunAuthSmsProvider()) {
            boolean pass = aliyunDypnsSmsVerifyService.verifyCode(phone, captchaInput);
            if (pass) {
                bucket.delete();
                return null;
            }
        }
        return "短信验证码错误或已过期";
    }

    private UserDO findUserByMobile(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getMobile, phone.trim())
                        .select(UserDO::getId, UserDO::getUsername, UserDO::getNickname,
                                UserDO::getPassword, UserDO::getMobile, UserDO::getStatus));
    }

    private String validateRegisterCaptcha(String uuid, String code) {
        if (!systemConfigHelper.isRegisterCaptchaEnabled()) {
            return null;
        }
        String captchaType = systemConfigHelper.getRegisterCaptchaType();
        String captchaInput = code == null ? "" : code.trim();
        if (SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(captchaType)) {
            if (!SystemConfigHelper.SLIDER_VERIFIED_CODE.equals(captchaInput)) {
                return "请完成滑块验证";
            }
            return null;
        }
        return validateImageCaptcha(uuid, captchaInput);
    }

    private String checkUserLoginStatus(UserDO user) {
        if (user.getStatus() == null || user.getStatus() == 1) {
            return null;
        }
        if (user.getStatus() == 0) {
            return "账号已停用，请联系管理员";
        }
        if (user.getStatus() == 2) {
            return "账号待审核，请等待管理员审核通过";
        }
        if (user.getStatus() == 3) {
            return "账号审核未通过，请联系管理员";
        }
        return "账号状态异常，无法登录";
    }

    private String validateImageCaptcha(String uuid, String captchaInput) {
        String captchaUuid = uuid == null ? "" : uuid.trim();
        if (captchaUuid.isEmpty() || captchaInput.isEmpty()) {
            return "请先完成图形验证码";
        }
        RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY + captchaUuid);
        String captchaCode = bucket.get();
        if (captchaCode == null || !captchaCode.equals(captchaInput.toLowerCase())) {
            return "验证码错误或已过期";
        }
        bucket.delete();
        return null;
    }

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
        int maxRetry = systemConfigHelper.getMaxRetryCount();
        long lockMinutes = systemConfigHelper.getLockTimeMinutes();
        RBucket<Integer> failBucket = redissonClient.getBucket(failKey);
        Integer failCount = failBucket.get();
        int nextCount = (failCount == null ? 0 : failCount) + 1;
        failBucket.set(nextCount, lockMinutes, TimeUnit.MINUTES);
        if (nextCount >= maxRetry) {
            redissonClient.getBucket(lockKey).set(System.currentTimeMillis(), lockMinutes, TimeUnit.MINUTES);
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
    public CommonResult<Map<String, Object>> info() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
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
        result.put("permissions", permissionService.getUserPermissionCodes(userId));
        return CommonResult.success(result);
    }
    
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public CommonResult<Boolean> logout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            onlineUserService.forceLogout(userId);
            tokenService.removeToken(userId);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "发送登录短信验证码")
    @PostMapping("/sms-code")
    public CommonResult<Boolean> sendSmsCode(@RequestBody SmsCodeReqVO reqVO, HttpServletRequest request) {
        if (!systemConfigHelper.isSmsEnabled()) {
            return CommonResult.error(400, "短信功能未启用");
        }
        if (!systemConfigHelper.isSmsLoginEnabled()) {
            return CommonResult.error(400, "当前未启用短信验证码登录");
        }
        String phone = reqVO.getPhone() == null ? "" : reqVO.getPhone().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return CommonResult.error(400, "请输入正确的手机号");
        }
        String clientIp = getClientIp(request);
        String rateErr = checkSmsSendRateLimit(phone, clientIp);
        if (rateErr != null) {
            return CommonResult.error(429, rateErr);
        }
        if (findUserByMobile(phone) == null) {
            return CommonResult.error(400, "该手机号未绑定任何账号");
        }
        if (systemConfigHelper.isSmsLoginSliderCaptchaEnabled()) {
            String captchaInput = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
            if (!SystemConfigHelper.SLIDER_VERIFIED_CODE.equals(captchaInput)) {
                return CommonResult.error(400, "请完成滑块验证");
            }
        }
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        boolean success = smsServiceFactory.sendCode(phone, code);
        if (!success) {
            return CommonResult.error(500, "短信发送失败，请稍后重试");
        }
        redissonClient.getBucket(SMS_CODE_KEY + phone).set(code, 5, TimeUnit.MINUTES);
        recordSmsSendOnSuccess(phone, clientIp);
        return CommonResult.success(true);
    }

    /** 短信发送前：IP/手机号 分钟级与每日限额 */
    private String checkSmsSendRateLimit(String phone, String clientIp) {
        String rlMsg = rateLimitByIp(clientIp, "sms-code", systemConfigHelper.getSmsPerIpMinute());
        if (rlMsg != null) {
            return rlMsg;
        }
        int ipDailyMax = systemConfigHelper.getSmsPerIpDaily();
        if (ipDailyMax > 0 && getDailyCount(SMS_DAILY_IP_KEY + clientIp) >= ipDailyMax) {
            return "当前 IP 今日短信发送次数已达上限，请明天再试";
        }
        int intervalSec = systemConfigHelper.getSmsSendIntervalSeconds();
        if (redissonClient.getBucket(SMS_LIMIT_KEY + phone).isExists()) {
            return "发送太频繁，请 " + intervalSec + " 秒后再试";
        }
        int phoneDailyMax = systemConfigHelper.getSmsPerPhoneDaily();
        if (phoneDailyMax > 0 && getDailyCount(SMS_DAILY_PHONE_KEY + phone) >= phoneDailyMax) {
            return "该手机号今日发送次数已达上限，请明天再试";
        }
        return null;
    }

    private void recordSmsSendOnSuccess(String phone, String clientIp) {
        int intervalSec = systemConfigHelper.getSmsSendIntervalSeconds();
        redissonClient.getBucket(SMS_LIMIT_KEY + phone).set("1", intervalSec, TimeUnit.SECONDS);
        incrementDailyCount(SMS_DAILY_PHONE_KEY + phone);
        incrementDailyCount(SMS_DAILY_IP_KEY + clientIp);
    }

    private long getDailyCount(String prefixKey) {
        return redissonClient.getAtomicLong(dailyKey(prefixKey)).get();
    }

    private void incrementDailyCount(String prefixKey) {
        int max = prefixKey.startsWith(SMS_DAILY_PHONE_KEY)
                ? systemConfigHelper.getSmsPerPhoneDaily()
                : systemConfigHelper.getSmsPerIpDaily();
        if (max <= 0) {
            return;
        }
        RAtomicLong counter = redissonClient.getAtomicLong(dailyKey(prefixKey));
        long n = counter.incrementAndGet();
        if (n == 1) {
            counter.expire(25, TimeUnit.HOURS);
        }
    }

    private String dailyKey(String prefixKey) {
        return prefixKey + LocalDate.now();
    }

    @Operation(summary = "核验短信验证码（阿里云短信认证）")
    @PostMapping("/sms-code/verify")
    public CommonResult<Boolean> verifySmsCode(@RequestBody SmsCodeVerifyReqVO reqVO) {
        if (!systemConfigHelper.isSmsEnabled()) {
            return CommonResult.error(400, "短信功能未启用");
        }
        String phone = reqVO.getPhone() == null ? "" : reqVO.getPhone().trim();
        String code = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return CommonResult.error(400, "请输入正确的手机号");
        }
        if (code.isBlank()) {
            return CommonResult.error(400, "请输入验证码");
        }
        if (systemConfigHelper.isAliyunAuthSmsProvider()) {
            boolean pass = aliyunDypnsSmsVerifyService.verifyCode(phone, code);
            if (!pass) {
                return CommonResult.error(400, "验证码错误或已过期");
            }
            redissonClient.getBucket(SMS_CODE_KEY + phone).delete();
            return CommonResult.success(true);
        }
        String cached = redissonClient.<String>getBucket(SMS_CODE_KEY + phone).get();
        if (cached == null || !cached.equalsIgnoreCase(code)) {
            return CommonResult.error(400, "验证码错误或已过期");
        }
        redissonClient.getBucket(SMS_CODE_KEY + phone).delete();
        return CommonResult.success(true);
    }
    
    @Operation(summary = "注册")
    @PostMapping("/register")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Boolean> register(@RequestBody RegisterReqVO reqVO, HttpServletRequest request) {
        if (!systemConfigHelper.isRegisterEnabled()) {
            return CommonResult.error(400, "系统暂未开放注册");
        }

        String clientIp = getClientIp(request);
        String rlMsg = rateLimitByIp(clientIp, "register", systemConfigHelper.getRegisterPerIpMinute());
        if (rlMsg != null) {
            return CommonResult.error(429, rlMsg);
        }

        String username = reqVO.getUsername() == null ? "" : reqVO.getUsername().trim();
        if (username.isEmpty()) {
            return CommonResult.error(400, "请输入用户名");
        }

        int minPwdLen = systemConfigHelper.getRegisterMinPasswordLength();
        String password = reqVO.getPassword() == null ? "" : reqVO.getPassword();
        if (password.length() < minPwdLen) {
            return CommonResult.error(400, "密码长度不能少于 " + minPwdLen + " 位");
        }

        String regCaptchaErr = validateRegisterCaptcha(reqVO.getUuid(), reqVO.getCode());
        if (regCaptchaErr != null) {
            return CommonResult.error(400, regCaptchaErr);
        }

        UserDO existing = userMapper.selectByUsernameRaw(username);
        if (existing != null) {
            if (existing.getDeleted() != null && existing.getDeleted() == 1) {
                return restoreAndRegister(existing, reqVO, password);
            }
            if (existing.getStatus() != null && existing.getStatus() == 2) {
                return CommonResult.error(400, "该用户名正在审核中，请等待管理员处理");
            }
            return CommonResult.error(400, "用户名已存在");
        }

        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(reqVO.getNickname() != null ? reqVO.getNickname() : username);
        user.setStatus(systemConfigHelper.isRegisterNeedAudit() ? 2 : 1);
        userMapper.insert(user);
        assignRegisterDefaultRole(user);
        if (systemConfigHelper.isRegisterNeedAudit()) {
            registerApprovalService.createOnRegister(user);
        }
        return buildRegisterSuccessResult();
    }

    /** 回收站软删后同用户名再次注册：恢复账号并更新资料 */
    private CommonResult<Boolean> restoreAndRegister(UserDO deletedUser, RegisterReqVO reqVO, String password) {
        if (deletedUser.getId() == null) {
            return CommonResult.error(400, "用户数据异常");
        }
        if (userMapper.restoreById(deletedUser.getId()) <= 0) {
            return CommonResult.error(400, "无法恢复该用户名，请联系管理员从回收站彻底删除后再注册");
        }
        UserDO user = userMapper.selectById(deletedUser.getId());
        if (user == null) {
            return CommonResult.error(500, "恢复账号失败，请稍后重试");
        }
        String username = deletedUser.getUsername();
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(reqVO.getNickname() != null ? reqVO.getNickname() : username);
        user.setStatus(systemConfigHelper.isRegisterNeedAudit() ? 2 : 1);
        userMapper.updateById(user);
        assignRegisterDefaultRole(user);
        if (systemConfigHelper.isRegisterNeedAudit()) {
            registerApprovalService.createOnRegister(user);
        }
        return buildRegisterSuccessResult();
    }

    private void assignRegisterDefaultRole(UserDO user) {
        if (user.getId() == null) {
            return;
        }
        String roleCode = systemConfigHelper.getRegisterDefaultRoleCode();
        RoleDO defaultRole = roleMapper.selectOne(
                new LambdaQueryWrapper<RoleDO>()
                        .eq(RoleDO::getCode, roleCode)
        );
        if (defaultRole != null) {
            Set<Long> roleIds = new HashSet<>();
            roleIds.add(defaultRole.getId());
            permissionService.assignUserRole(user.getId(), roleIds);
        }
    }

    private CommonResult<Boolean> buildRegisterSuccessResult() {
        CommonResult<Boolean> result = CommonResult.success(true);
        result.setMessage(systemConfigHelper.isRegisterNeedAudit()
                ? "注册成功，请等待管理员审核"
                : "注册成功");
        return result;
    }
    
    @Data
    public static class LoginReqVO {
        /** account | sms */
        private String loginType;
        private String username;
        private String password;
        private String phone;
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

    @Data
    public static class SmsCodeReqVO {
        private String phone;
        /** 滑块验证通过时传 slider_verified */
        private String code;
    }

    @Data
    public static class SmsCodeVerifyReqVO {
        private String phone;
        private String code;
    }
}