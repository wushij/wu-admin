package com.admin.server.modules.system.service.auth;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthForgotPasswordService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private SystemConfigHelper systemConfigHelper;
    @Resource
    private ProfileSmsPasswordService profileSmsPasswordService;
    @Resource
    private RedissonClient redissonClient;

    public UserDO resolveUser(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return userMapper.selectByUsernameRaw(username.trim());
    }

    public String validateUserForForgot(UserDO user) {
        if (user == null) {
            return "用户不存在";
        }
        if ("zhangsan".equalsIgnoreCase(user.getUsername())) {
            return "演示体验账号禁止找回密码";
        }
        String statusErr = checkUserStatus(user);
        if (statusErr != null) {
            return statusErr;
        }
        if (!systemConfigHelper.isSmsEnabled()) {
            return "短信功能未启用，请联系管理员";
        }
        if (normalizeMobile(user.getMobile()) == null) {
            return "该账号未绑定手机号，请联系管理员";
        }
        return null;
    }

    public String maskMobile(String mobile) {
        String phone = normalizeMobile(mobile);
        if (phone == null) {
            return "";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    public String sendResetCode(UserDO user, String clientIp, String sliderToken, String offsetXStr) {
        return profileSmsPasswordService.sendResetCode(user, clientIp, sliderToken, offsetXStr);
    }

    public String resetPasswordBySms(UserDO user, String smsCode, String newPassword, String confirmPassword) {
        return profileSmsPasswordService.resetPasswordBySms(user, smsCode, newPassword, confirmPassword);
    }

    /**
     * 短信重置密码并持久化：内部先校验+改密（仅改内存），成功后再 updateById 落库，
     * 保证校验失败不会误持久化。事务覆盖落库阶段。
     */
    @Transactional(rollbackFor = Exception.class)
    public String resetPasswordAndSave(UserDO user, String smsCode, String newPassword, String confirmPassword) {
        String err = resetPasswordBySms(user, smsCode, newPassword, confirmPassword);
        if (err != null) {
            return err;
        }
        userMapper.updateById(user);
        return null;
    }

    public String rateLimitCheck(String clientIp) {
        return rateLimitByIp(clientIp, "forgot-pwd-check", systemConfigHelper.getSmsPerIpMinute());
    }

    private String checkUserStatus(UserDO user) {
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
        return "账号状态异常，请联系管理员";
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

    private String rateLimitByIp(String clientIp, String action, int maxPerMinute) {
        if (maxPerMinute <= 0) {
            return null;
        }
        long minute = System.currentTimeMillis() / 60_000L;
        String redisKey = "auth:rl:" + action + ":" + clientIp + ":" + minute;
        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);
        long n = counter.incrementAndGet();
        if (n == 1) {
            counter.expire(Duration.ofSeconds(90));
        }
        if (n > maxPerMinute) {
            return "请求过于频繁，请稍后再试";
        }
        return null;
    }
}
