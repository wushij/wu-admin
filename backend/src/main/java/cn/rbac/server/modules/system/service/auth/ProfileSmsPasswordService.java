package cn.rbac.server.modules.system.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.sms.AliyunDypnsSmsVerifyService;
import cn.rbac.server.modules.system.sms.SmsServiceFactory;
import jakarta.annotation.Resource;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 个人中心：短信验证重置密码（发码前强制滑块，不走系统配置开关）
 */
@Service
public class ProfileSmsPasswordService {

    private static final String PROFILE_RESET_SMS_KEY = "sms:profile-reset:";
    private static final String SMS_LIMIT_KEY = "sms:limit:";
    private static final String SMS_DAILY_PHONE_KEY = "sms:daily:phone:";
    private static final String SMS_DAILY_IP_KEY = "sms:daily:ip:";

    @Resource
    private SystemConfigHelper systemConfigHelper;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private SmsServiceFactory smsServiceFactory;
    @Resource
    private AliyunDypnsSmsVerifyService aliyunDypnsSmsVerifyService;
    @Resource
    private PasswordEncoder passwordEncoder;

    public String sendResetCode(UserDO user, String clientIp, String sliderCode) {
        if (user == null || user.getId() == null) {
            return "用户不存在";
        }
        if (!systemConfigHelper.isSmsEnabled()) {
            return "短信功能未启用，请联系管理员";
        }
        String phone = normalizeMobile(user.getMobile());
        if (phone == null) {
            return "请先在基本资料中绑定手机号";
        }
        String captchaInput = sliderCode == null ? "" : sliderCode.trim();
        if (!SystemConfigHelper.SLIDER_VERIFIED_CODE.equals(captchaInput)) {
            return "请完成滑块验证";
        }
        String rateErr = checkSmsSendRateLimit(phone, clientIp);
        if (rateErr != null) {
            return rateErr;
        }
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        String template = systemConfigHelper.getSmsTemplateResetPassword();
        if (StrUtil.isBlank(template)) {
            template = systemConfigHelper.getSmsTemplateVerifyCode();
        }
        boolean success = StrUtil.isBlank(template)
                ? smsServiceFactory.sendCode(phone, code)
                : smsServiceFactory.sendCode(phone, code, template);
        if (!success) {
            return "短信发送失败，请稍后重试";
        }
        int expireMin = systemConfigHelper.getSmsCodeExpireMinutes();
        redissonClient.getBucket(PROFILE_RESET_SMS_KEY + user.getId()).set(code, expireMin, TimeUnit.MINUTES);
        recordSmsSendOnSuccess(phone, clientIp);
        return null;
    }

    public String resetPasswordBySms(UserDO user, String smsCode, String newPassword, String confirmPassword) {
        if (user == null || user.getId() == null) {
            return "用户不存在";
        }
        if (!systemConfigHelper.isSmsEnabled()) {
            return "短信功能未启用，请联系管理员";
        }
        String phone = normalizeMobile(user.getMobile());
        if (phone == null) {
            return "请先在基本资料中绑定手机号";
        }
        String code = smsCode == null ? "" : smsCode.trim();
        if (code.isEmpty()) {
            return "请输入短信验证码";
        }
        int minLen = systemConfigHelper.getRegisterMinPasswordLength();
        if (newPassword == null || newPassword.length() < minLen) {
            return "新密码长度不能少于 " + minLen + " 位";
        }
        if (StrUtil.isNotBlank(confirmPassword) && !newPassword.equals(confirmPassword)) {
            return "两次输入的新密码不一致";
        }
        String verifyErr = verifySmsCode(user.getId(), phone, code);
        if (verifyErr != null) {
            return verifyErr;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        return null;
    }

    private String verifySmsCode(Long userId, String phone, String code) {
        RBucket<String> bucket = redissonClient.getBucket(PROFILE_RESET_SMS_KEY + userId);
        String cached = bucket.get();
        if (cached != null && cached.equalsIgnoreCase(code)) {
            bucket.delete();
            return null;
        }
        if (systemConfigHelper.isAliyunAuthSmsProvider()) {
            boolean pass = aliyunDypnsSmsVerifyService.verifyCode(phone, code);
            if (pass) {
                bucket.delete();
                return null;
            }
        }
        return "短信验证码不正确，请重新获取";
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        String phone = mobile.trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return null;
        }
        return phone;
    }

    private String checkSmsSendRateLimit(String phone, String clientIp) {
        String rlMsg = rateLimitByIp(clientIp, "profile-pwd-sms", systemConfigHelper.getSmsPerIpMinute());
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
}
