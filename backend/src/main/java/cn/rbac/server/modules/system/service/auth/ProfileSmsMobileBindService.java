package cn.rbac.server.modules.system.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.sms.AliyunDypnsSmsVerifyService;
import cn.rbac.server.modules.system.sms.SmsServiceFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 个人中心：短信验证绑定/更换手机号（发码前强制滑块）
 */
@Service
public class ProfileSmsMobileBindService {

    private static final String PROFILE_BIND_SMS_KEY = "sms:profile-mobile-bind:";
    private static final String PROFILE_BIND_PHONE_KEY = "sms:profile-mobile-bind:phone:";
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
    private UserMapper userMapper;

    public String sendBindCode(Long userId, String newMobile, String clientIp, String sliderCode) {
        if (userId == null) {
            return "用户不存在";
        }
        if (!systemConfigHelper.isSmsEnabled()) {
            return "短信功能未启用，请联系管理员";
        }
        String phone = normalizeMobile(newMobile);
        if (phone == null) {
            return "请输入正确的手机号";
        }
        UserDO self = userMapper.selectById(userId);
        if (self != null && phone.equals(normalizeMobile(self.getMobile()))) {
            return "新手机号不能与当前绑定的号码相同";
        }
        UserDO exist = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getMobile, phone)
                .ne(UserDO::getId, userId)
                .eq(UserDO::getDeleted, 0));
        if (exist != null) {
            return "该手机号已被其他账号使用";
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
        String template = resolveBindTemplate(self);
        boolean success = StrUtil.isBlank(template)
                ? smsServiceFactory.sendCode(phone, code)
                : smsServiceFactory.sendCode(phone, code, template);
        if (!success) {
            return "短信发送失败，请稍后重试";
        }
        int expireMin = systemConfigHelper.getSmsCodeExpireMinutes();
        redissonClient.getBucket(PROFILE_BIND_SMS_KEY + userId).set(code, expireMin, TimeUnit.MINUTES);
        redissonClient.getBucket(PROFILE_BIND_PHONE_KEY + userId).set(phone, expireMin, TimeUnit.MINUTES);
        recordSmsSendOnSuccess(phone, clientIp);
        return null;
    }

    public String bindMobile(Long userId, UserDO user, String newMobile, String smsCode) {
        if (user == null || userId == null) {
            return "用户不存在";
        }
        if (!systemConfigHelper.isSmsEnabled()) {
            return "短信功能未启用，请联系管理员";
        }
        String phone = normalizeMobile(newMobile);
        if (phone == null) {
            return "请输入正确的手机号";
        }
        String code = smsCode == null ? "" : smsCode.trim();
        if (code.isEmpty()) {
            return "请输入短信验证码";
        }
        UserDO exist = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getMobile, phone)
                .ne(UserDO::getId, userId)
                .eq(UserDO::getDeleted, 0));
        if (exist != null) {
            return "该手机号已被其他账号使用";
        }
        String verifyErr = verifyBindCode(userId, phone, code);
        if (verifyErr != null) {
            return verifyErr;
        }
        user.setMobile(phone);
        return null;
    }

    private String resolveBindTemplate(UserDO user) {
        boolean hasMobile = user != null && normalizeMobile(user.getMobile()) != null;
        String template = hasMobile
                ? systemConfigHelper.getSmsTemplateModifyPhone()
                : systemConfigHelper.getSmsTemplateBindPhone();
        if (StrUtil.isBlank(template)) {
            template = systemConfigHelper.getSmsTemplateVerifyCode();
        }
        return template;
    }

    private String verifyBindCode(Long userId, String phone, String code) {
        RBucket<String> phoneBucket = redissonClient.getBucket(PROFILE_BIND_PHONE_KEY + userId);
        String pendingPhone = phoneBucket.get();
        if (pendingPhone == null || !pendingPhone.equals(phone)) {
            return "请先获取该手机号的验证码";
        }
        RBucket<String> codeBucket = redissonClient.getBucket(PROFILE_BIND_SMS_KEY + userId);
        String cached = codeBucket.get();
        if (cached != null && cached.equalsIgnoreCase(code)) {
            codeBucket.delete();
            phoneBucket.delete();
            return null;
        }
        if (systemConfigHelper.isAliyunAuthSmsProvider()) {
            boolean pass = aliyunDypnsSmsVerifyService.verifyCode(phone, code);
            if (pass) {
                codeBucket.delete();
                phoneBucket.delete();
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
        String rlMsg = rateLimitByIp(clientIp, "profile-bind-mobile", systemConfigHelper.getSmsPerIpMinute());
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
