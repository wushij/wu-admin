package cn.rbac.server.modules.system.service.auth.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.util.IpLocationUtils;
import cn.rbac.server.common.util.UserAgentUtils;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.modules.system.api.auth.vo.LoginReqVO;
import cn.rbac.server.modules.system.api.auth.vo.RegisterReqVO;
import cn.rbac.server.modules.system.api.auth.vo.SmsCodeReqVO;
import cn.rbac.server.modules.system.api.auth.vo.SmsCodeVerifyReqVO;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.approval.RegisterApprovalService;
import cn.rbac.server.modules.system.service.auth.AuthService;
import cn.rbac.server.modules.system.service.auth.LoginLockService;
import cn.rbac.server.modules.system.service.auth.SliderCaptchaService;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.loginlog.LoginLogService;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.rbac.server.modules.system.sms.AliyunDypnsSmsVerifyService;
import cn.rbac.server.modules.system.sms.SmsServiceFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource private UserMapper userMapper;
    @Resource private PasswordEncoder passwordEncoder;
    @Resource private TokenService tokenService;
    @Resource private PermissionService permissionService;
    @Resource private RedissonClient redissonClient;
    @Resource private RoleMapper roleMapper;
    @Resource private LoginLogService loginLogService;
    @Resource private OnlineUserService onlineUserService;
    @Resource private SystemConfigHelper systemConfigHelper;
    @Resource private RegisterApprovalService registerApprovalService;
    @Resource private SmsServiceFactory smsServiceFactory;
    @Resource private AliyunDypnsSmsVerifyService aliyunDypnsSmsVerifyService;
    @Resource private LoginLockService loginLockService;
    @Resource private SliderCaptchaService sliderCaptchaService;

    private static final String CAPTCHA_KEY = "captcha:";
    private static final String SMS_CODE_KEY = "sms:login:";
    private static final String SMS_LIMIT_KEY = "sms:limit:";
    private static final String SMS_DAILY_PHONE_KEY = "sms:daily:phone:";
    private static final String SMS_DAILY_IP_KEY = "sms:daily:ip:";

    @Override
    public Map<String, Object> generateCaptcha(String scene, String clientIp) {
        boolean captchaEnabled = "register".equalsIgnoreCase(scene)
                ? systemConfigHelper.isRegisterCaptchaEnabled()
                : systemConfigHelper.isCaptchaEnabled();
        if (!captchaEnabled) {
            throw new BusinessException(400, "当前未启用验证码");
        }
        String captchaType = "register".equalsIgnoreCase(scene)
                ? systemConfigHelper.getRegisterCaptchaType()
                : systemConfigHelper.getCaptchaType();
        if (SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(captchaType)) {
            throw new BusinessException(400, "当前为滑块验证码，无需拉取图片验证码");
        }
        String rlMsg = rateLimitByIp(clientIp, "captcha", systemConfigHelper.getCaptchaPerIpMinute());
        if (rlMsg != null) {
            throw new BusinessException(429, rlMsg);
        }
        String uuid = IdUtil.simpleUUID();
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 50);
        String code = lineCaptcha.getCode();
        String imageBase64 = lineCaptcha.getImageBase64();
        RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY + uuid);
        setWithTtl(bucket, code.toLowerCase(), 5, TimeUnit.MINUTES);
        Map<String, Object> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("img", imageBase64);
        return result;
    }

    @Override
    public Map<String, Object> createSliderChallenge(String scene, String clientIp) {
        return sliderCaptchaService.createChallenge(scene, clientIp);
    }

    @Override
    public Map<String, Object> getPublicConfig() {
        return systemConfigHelper.buildPublicConfig();
    }

    @Override
    public Map<String, Object> loginByAccount(LoginReqVO reqVO, String clientIp, String userAgent) {
        String username = reqVO.getUsername() == null ? "" : reqVO.getUsername().trim();
        String rlMsg = rateLimitByIp(clientIp, "login", systemConfigHelper.getLoginPerIpMinute());
        if (rlMsg != null) {
            recordLoginLog(null, username, 1, rlMsg, clientIp, userAgent);
            throw new BusinessException(429, rlMsg);
        }
        String lockMessage = loginLockService.checkLoginLockMessage(username, clientIp);
        if (lockMessage != null) {
            recordLoginLog(null, username, 1, lockMessage, clientIp, userAgent);
            throw new BusinessException(429, lockMessage);
        }
        String captchaErr = validateLoginCaptcha(reqVO);
        if (captchaErr != null) {
            loginLockService.recordLoginFailure(username, clientIp);
            recordLoginLog(null, username, 1, captchaErr, clientIp, userAgent);
            throw new BusinessException(400, captchaErr);
        }
        UserDO user = userMapper.selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
        if (user == null) {
            loginLockService.recordLoginFailure(username, clientIp);
            recordLoginLog(null, username, 1, "用户不存在", clientIp, userAgent);
            throw new BusinessException(400, "账号或密码错误");
        }
        if (!passwordEncoder.matches(reqVO.getPassword(), user.getPassword())) {
            loginLockService.recordLoginFailure(username, clientIp);
            recordLoginLog(user.getId(), username, 1, "密码错误", clientIp, userAgent);
            throw new BusinessException(400, "账号或密码错误");
        }
        String statusErr = checkUserLoginStatus(user);
        if (statusErr != null) {
            recordLoginLog(user.getId(), username, 1, statusErr, clientIp, userAgent);
            throw new BusinessException(403, statusErr);
        }
        return completeLogin(user, clientIp, userAgent);
    }

    @Override
    public Map<String, Object> loginBySms(LoginReqVO reqVO, String clientIp, String userAgent) {
        if (!systemConfigHelper.isSmsLoginEnabled()) {
            throw new BusinessException(400, "当前未启用短信登录");
        }
        if (!systemConfigHelper.isSmsEnabled()) {
            throw new BusinessException(400, "短信功能未启用");
        }
        String phone = reqVO.getPhone() == null ? "" : reqVO.getPhone().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请输入正确的手机号");
        }
        UserDO user = findUserByMobile(phone);
        String username = user != null ? user.getUsername() : phone;
        String rlMsg = rateLimitByIp(clientIp, "login", systemConfigHelper.getLoginPerIpMinute());
        if (rlMsg != null) {
            recordLoginLog(null, username, 1, rlMsg, clientIp, userAgent);
            throw new BusinessException(429, rlMsg);
        }
        String lockMessage = loginLockService.checkLoginLockMessage(username, clientIp);
        if (lockMessage != null) {
            recordLoginLog(null, username, 1, lockMessage, clientIp, userAgent);
            throw new BusinessException(429, lockMessage);
        }
        String captchaErr = validateSmsLoginCaptcha(reqVO);
        if (captchaErr != null) {
            loginLockService.recordLoginFailure(username, clientIp);
            recordLoginLog(null, username, 1, captchaErr, clientIp, userAgent);
            throw new BusinessException(400, captchaErr);
        }
        if (user == null) {
            loginLockService.recordLoginFailure(username, clientIp);
            recordLoginLog(null, username, 1, "该手机号未绑定任何账号", clientIp, userAgent);
            throw new BusinessException(400, "该手机号未绑定任何账号");
        }
        String statusErr = checkUserLoginStatus(user);
        if (statusErr != null) {
            recordLoginLog(user.getId(), username, 1, statusErr, clientIp, userAgent);
            throw new BusinessException(403, statusErr);
        }
        return completeLogin(user, clientIp, userAgent);
    }

    @Override
    public void sendSmsCode(SmsCodeReqVO reqVO, String clientIp) {
        if (!systemConfigHelper.isSmsEnabled()) {
            throw new BusinessException(400, "短信功能未启用");
        }
        if (!systemConfigHelper.isSmsLoginEnabled()) {
            throw new BusinessException(400, "当前未启用短信验证码登录");
        }
        String phone = reqVO.getPhone() == null ? "" : reqVO.getPhone().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请输入正确的手机号");
        }
        String rateErr = checkSmsSendRateLimit(phone, clientIp);
        if (rateErr != null) {
            throw new BusinessException(429, rateErr);
        }
        if (findUserByMobile(phone) == null) {
            throw new BusinessException(400, "该手机号未绑定任何账号");
        }
        if (systemConfigHelper.isSmsLoginSliderCaptchaEnabled()) {
            String sliderErr = sliderCaptchaService.verifyAndConsume(reqVO.getUuid(), reqVO.getCode());
            if (sliderErr != null) {
                throw new BusinessException(400, sliderErr);
            }
        }
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        boolean success = smsServiceFactory.sendCode(phone, code);
        if (!success) {
            throw new BusinessException(500, "短信发送失败，请稍后重试");
        }
        setWithTtl(redissonClient.getBucket(SMS_CODE_KEY + phone), code, 5, TimeUnit.MINUTES);
        recordSmsSendOnSuccess(phone, clientIp);
    }

    @Override
    public boolean verifySmsCode(SmsCodeVerifyReqVO reqVO) {
        if (!systemConfigHelper.isSmsEnabled()) {
            throw new BusinessException(400, "短信功能未启用");
        }
        String phone = reqVO.getPhone().trim();
        String code = reqVO.getCode().trim();
        if (systemConfigHelper.isAliyunAuthSmsProvider()) {
            boolean pass = aliyunDypnsSmsVerifyService.verifyCode(phone, code);
            if (!pass) {
                throw new BusinessException(400, "验证码错误或已过期");
            }
            redissonClient.getBucket(SMS_CODE_KEY + phone).delete();
            return true;
        }
        String cached = redissonClient.<String>getBucket(SMS_CODE_KEY + phone).get();
        if (cached == null || !cached.equalsIgnoreCase(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        redissonClient.getBucket(SMS_CODE_KEY + phone).delete();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(RegisterReqVO reqVO, String clientIp) {
        if (!systemConfigHelper.isRegisterEnabled()) {
            throw new BusinessException(400, "系统暂未开放注册");
        }
        String rlMsg = rateLimitByIp(clientIp, "register", systemConfigHelper.getRegisterPerIpMinute());
        if (rlMsg != null) {
            throw new BusinessException(429, rlMsg);
        }
        String username = reqVO.getUsername().trim();
        int minPwdLen = systemConfigHelper.getRegisterMinPasswordLength();
        String password = reqVO.getPassword();
        if (password.length() < minPwdLen) {
            throw new BusinessException(400, "密码长度不能少于 " + minPwdLen + " 位");
        }
        String regCaptchaErr = validateRegisterCaptcha(reqVO.getUuid(), reqVO.getCode());
        if (regCaptchaErr != null) {
            throw new BusinessException(400, regCaptchaErr);
        }
        UserDO existing = userMapper.selectByUsernameRaw(username);
        if (existing != null) {
            if (existing.getDeleted() != null && existing.getDeleted() == 1) {
                restoreAndRegister(existing, reqVO, password);
                return systemConfigHelper.isRegisterNeedAudit();
            }
            if (existing.getStatus() != null && existing.getStatus() == 2) {
                throw new BusinessException(400, "该用户名正在审核中，请等待管理员处理");
            }
            throw new BusinessException(400, "用户名已存在");
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
        return systemConfigHelper.isRegisterNeedAudit();
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        UserDO user = userMapper.selectById(userId);
        onlineUserService.touchLastAccess(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("roles", permissionService.getUserRoleIdListByUserId(userId));
        result.put("menus", permissionService.getUserMenuList(userId));
        result.put("permissions", permissionService.getUserPermissionCodes(userId));
        return result;
    }

    @Override
    public void logout(Long userId) {
        onlineUserService.forceLogout(userId);
        tokenService.removeToken(userId);
    }

    // ====== 私有方法 ======

    private Map<String, Object> completeLogin(UserDO user, String clientIp, String userAgent) {
        String username = user.getUsername();
        loginLockService.clearLoginFailure(username, clientIp);
        tokenService.createToken(user.getId(), user.getUsername());
        StpUtil.getSession().set(TokenService.SESSION_NICKNAME, user.getNickname());
        onlineUserService.recordLoginSession(user.getId(), user.getUsername(), user.getNickname(), clientIp, userAgent);
        recordLoginLog(user.getId(), username, 0, "登录成功", clientIp, userAgent);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("token", StpUtil.getTokenValue());
        return result;
    }

    private void recordLoginLog(Long userId, String username, Integer status, String msg, String ip, String userAgent) {
        if (userId == null && StringUtils.hasText(username)) {
            UserDO u = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                    .eq(UserDO::getUsername, username).select(UserDO::getId));
            if (u != null) {
                userId = u.getId();
            }
        }
        LoginLogDO logEntry = new LoginLogDO();
        logEntry.setUserId(userId);
        logEntry.setUsername(username);
        logEntry.setStatus(status);
        logEntry.setMsg(msg);
        logEntry.setLoginTime(LocalDateTime.now());
        logEntry.setIpaddr(ip);
        logEntry.setLoginLocation(IpLocationUtils.resolve(ip));
        logEntry.setBrowser(UserAgentUtils.parseBrowser(userAgent));
        logEntry.setOs(UserAgentUtils.parseOsFromUserAgent(userAgent));
        loginLogService.recordAsync(logEntry);
    }

    private String validateLoginCaptcha(LoginReqVO reqVO) {
        if (!systemConfigHelper.isCaptchaEnabled()) return null;
        String captchaType = systemConfigHelper.getCaptchaType();
        String captchaInput = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
        if (SystemConfigHelper.CAPTCHA_TYPE_SMS.equals(captchaType)) return null;
        if (SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(captchaType)) {
            return sliderCaptchaService.verifyAndConsume(reqVO.getUuid(), reqVO.getCode());
        }
        return validateImageCaptcha(reqVO.getUuid(), captchaInput);
    }

    private String validateSmsLoginCaptcha(LoginReqVO reqVO) {
        if (!systemConfigHelper.isSmsEnabled()) return "短信功能未启用";
        String phone = reqVO.getPhone() == null ? "" : reqVO.getPhone().trim();
        String captchaInput = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) return "请输入正确的手机号";
        if (captchaInput.isEmpty()) return "请输入短信验证码";
        if (findUserByMobile(phone) == null) return "该手机号未绑定任何账号";
        RBucket<String> bucket = redissonClient.getBucket(SMS_CODE_KEY + phone);
        String cached = bucket.get();
        if (cached != null && cached.equalsIgnoreCase(captchaInput)) {
            bucket.delete();
            return null;
        }
        if (systemConfigHelper.isAliyunAuthSmsProvider()) {
            boolean pass = aliyunDypnsSmsVerifyService.verifyCode(phone, captchaInput);
            if (pass) { bucket.delete(); return null; }
        }
        return "短信验证码错误或已过期";
    }

    private UserDO findUserByMobile(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getMobile, phone.trim())
                .select(UserDO::getId, UserDO::getUsername, UserDO::getNickname,
                        UserDO::getPassword, UserDO::getMobile, UserDO::getStatus));
    }

    private String validateRegisterCaptcha(String uuid, String code) {
        if (!systemConfigHelper.isRegisterCaptchaEnabled()) return null;
        String captchaType = systemConfigHelper.getRegisterCaptchaType();
        String captchaInput = code == null ? "" : code.trim();
        if (SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(captchaType)) {
            return sliderCaptchaService.verifyAndConsume(uuid, code);
        }
        return validateImageCaptcha(uuid, captchaInput);
    }

    private String checkUserLoginStatus(UserDO user) {
        if (user.getStatus() == null || user.getStatus() == 1) return null;
        if (user.getStatus() == 0) return "账号已停用，请联系管理员";
        if (user.getStatus() == 2) return "账号待审核，请等待管理员审核通过";
        if (user.getStatus() == 3) return "账号审核未通过，请联系管理员";
        return "账号状态异常，无法登录";
    }

    private String validateImageCaptcha(String uuid, String captchaInput) {
        String captchaUuid = uuid == null ? "" : uuid.trim();
        if (captchaUuid.isEmpty() || captchaInput.isEmpty()) return "请先完成图形验证码";
        RBucket<String> bucket = redissonClient.getBucket(CAPTCHA_KEY + captchaUuid);
        String captchaCode = bucket.get();
        if (captchaCode == null || !captchaCode.equals(captchaInput.toLowerCase())) return "验证码错误或已过期";
        bucket.delete();
        return null;
    }

    private String rateLimitByIp(String clientIp, String action, int maxPerMinute) {
        if (maxPerMinute <= 0) return null;
        long minute = System.currentTimeMillis() / 60_000L;
        String redisKey = "auth:rl:" + action + ":" + clientIp + ":" + minute;
        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);
        long n = counter.incrementAndGet();
        if (n == 1) { expireAfter(counter, 90, TimeUnit.SECONDS); }
        if (n > maxPerMinute) return "请求过于频繁，请稍后再试";
        return null;
    }

    @SuppressWarnings("deprecation")
    private <V> void setWithTtl(RBucket<V> bucket, V value, long duration, TimeUnit unit) {
        bucket.set(value, duration, unit);
    }

    private void expireAfter(org.redisson.api.RExpirable expirable, long duration, TimeUnit unit) {
        expirable.expire(Duration.of(duration, unit.toChronoUnit()));
    }

    private String checkSmsSendRateLimit(String phone, String clientIp) {
        String rlMsg = rateLimitByIp(clientIp, "sms-code", systemConfigHelper.getSmsPerIpMinute());
        if (rlMsg != null) return rlMsg;
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
        setWithTtl(redissonClient.getBucket(SMS_LIMIT_KEY + phone), "1", intervalSec, TimeUnit.SECONDS);
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
        if (max <= 0) return;
        RAtomicLong counter = redissonClient.getAtomicLong(dailyKey(prefixKey));
        long n = counter.incrementAndGet();
        if (n == 1) { expireAfter(counter, 25, TimeUnit.HOURS); }
    }

    private String dailyKey(String prefixKey) {
        return prefixKey + LocalDate.now();
    }

    private void restoreAndRegister(UserDO deletedUser, RegisterReqVO reqVO, String password) {
        if (deletedUser.getId() == null) {
            throw new BusinessException(400, "用户数据异常");
        }
        if (userMapper.restoreById(deletedUser.getId()) <= 0) {
            throw new BusinessException(400, "无法恢复该用户名，请联系管理员从回收站彻底删除后再注册");
        }
        UserDO user = userMapper.selectById(deletedUser.getId());
        if (user == null) {
            throw new BusinessException(500, "恢复账号失败，请稍后重试");
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
    }

    private void assignRegisterDefaultRole(UserDO user) {
        if (user.getId() == null) return;
        String roleCode = systemConfigHelper.getRegisterDefaultRoleCode();
        RoleDO defaultRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, roleCode));
        if (defaultRole != null) {
            Set<Long> roleIds = new HashSet<>();
            roleIds.add(defaultRole.getId());
            permissionService.assignUserRole(user.getId(), roleIds);
        }
    }
}