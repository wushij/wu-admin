package com.admin.server.modules.system.service.email;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class EmailCodeService {

    private static final String EMAIL_CODE_KEY = "email:code:";
    private static final String EMAIL_LIMIT_KEY = "email:limit:";
    private static final String EMAIL_DAILY_KEY = "email:daily:";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SystemConfigHelper systemConfigHelper;

    /**
     * 发送邮箱验证码（默认绑定场景）
     */
    public String sendEmailCode(String toEmail) {
        return sendEmailCode(toEmail, "bind");
    }

    /**
     * 发送邮箱验证码（支持指定业务场景：register / login / bind / resetPwd）
     * @param toEmail 目标邮箱
     * @param scene 场景代码 (register, login, bind, resetPwd)
     * @return 错误提示消息，为 null 表示成功
     */
    public String sendEmailCode(String toEmail, String scene) {
        if (!systemConfigHelper.isEmailEnabled()) {
            return "邮件服务已关闭，请联系管理员开启";
        }
        if (StrUtil.isBlank(toEmail)) {
            return "邮箱不能为空";
        }
        String email = toEmail.trim();
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "邮箱格式不正确";
        }

        // 频率间隔校验
        int intervalSec = systemConfigHelper.getEmailSendIntervalSeconds();
        RBucket<String> limitBucket = redissonClient.getBucket(EMAIL_LIMIT_KEY + email);
        if (limitBucket.isExists()) {
            return "发送过于频繁，请 " + intervalSec + " 秒后再试";
        }

        // 每日发送次数上限校验
        int dailyLimit = systemConfigHelper.getEmailDailyLimitPerEmail();
        RBucket<Integer> dailyBucket = redissonClient.getBucket(EMAIL_DAILY_KEY + email + ":" + DateUtil.today());
        Integer currentCount = dailyBucket.get();
        if (currentCount != null && currentCount >= dailyLimit) {
            return "该邮箱今日发送次数已达上限（" + dailyLimit + "次），请明天后再试";
        }

        // 验证码配置
        int codeLen = systemConfigHelper.getEmailCodeLength();
        int expireMin = systemConfigHelper.getEmailCodeExpireMinutes();
        String code = RandomUtil.randomNumbers(codeLen);

        // 存储验证码
        RBucket<String> codeBucket = redissonClient.getBucket(EMAIL_CODE_KEY + email);
        codeBucket.set(code, Duration.ofMinutes(expireMin));

        // 设置频率锁与每日计数
        limitBucket.set("1", Duration.ofSeconds(intervalSec));
        dailyBucket.set(currentCount == null ? 1 : currentCount + 1, Duration.ofDays(1));

        // 组装场景特定的邮件主题与正文
        String platformName = systemConfigHelper.getPlatformName();
        String subject;
        String actionText;
        String normalizedScene = StrUtil.isBlank(scene) ? "bind" : scene.trim().toLowerCase();

        switch (normalizedScene) {
            case "register":
                subject = "【" + platformName + "】账号注册验证码";
                actionText = "您正在注册【" + platformName + "】账号";
                break;
            case "login":
                subject = "【" + platformName + "】快捷登录验证码";
                actionText = "您正在登录【" + platformName + "】系统";
                break;
            case "resetpwd":
            case "modifypwd":
                subject = "【" + platformName + "】重置密码验证码";
                actionText = "您正在申请重置/修改密码";
                break;
            case "bind":
            default:
                subject = "【" + platformName + "】绑定邮箱验证码";
                actionText = "您正在进行邮箱绑定/更换操作";
                break;
        }

        String mailText = "您好！" + actionText + "，您的验证码为：" + code + "，有效期 " + expireMin + " 分钟。如非本人操作请忽略。";

        // 异步投递邮件
        CompletableFuture.runAsync(() -> {
            try {
                JavaMailSenderImpl sender = systemConfigHelper.buildDynamicMailSender();
                SimpleMailMessage message = new SimpleMailMessage();
                String from = systemConfigHelper.getEmailUsername();
                String fromName = systemConfigHelper.getEmailFromName();
                if (StrUtil.isNotBlank(fromName)) {
                    message.setFrom(fromName + " <" + from + ">");
                } else {
                    message.setFrom(from);
                }
                message.setTo(email);
                message.setSubject(subject);
                message.setText(mailText);

                sender.send(message);
                log.info("【{}】场景邮件验证码已成功发送至 {}", normalizedScene, email);
            } catch (Exception e) {
                log.error("发送【{}】场景邮件验证码至 {} 失败", normalizedScene, email, e);
            }
        });

        return null;
    }

    /**
     * 发送测试邮件（同步发送以便获取错误提示）
     */
    public String sendTestEmail(String toEmail) {
        if (!systemConfigHelper.isEmailEnabled()) {
            return "邮件服务已关闭，请先开启【邮件功能】开关";
        }
        try {
            JavaMailSenderImpl sender = systemConfigHelper.buildDynamicMailSender();
            SimpleMailMessage message = new SimpleMailMessage();
            String from = systemConfigHelper.getEmailUsername();
            String fromName = systemConfigHelper.getEmailFromName();
            if (StrUtil.isNotBlank(fromName)) {
                message.setFrom(fromName + " <" + from + ">");
            } else {
                message.setFrom(from);
            }
            message.setTo(toEmail);
            message.setSubject("【" + systemConfigHelper.getPlatformName() + "】系统配置邮件服务测试");
            message.setText("您好！收到此邮件说明【" + systemConfigHelper.getPlatformName() + "】系统的 SMTP 邮件服务配置正确且网络连通成功。\n发送时间：" + DateUtil.now());

            sender.send(message);
            log.info("测试邮件已成功发送至 {}", toEmail);
            return null;
        } catch (Exception e) {
            log.error("测试邮件发送至 {} 失败", toEmail, e);
            return e.getMessage();
        }
    }

    /**
     * 校验邮箱验证码
     * @return 错误提示消息，为 null 表示成功
     */
    public String verifyEmailCode(String toEmail, String inputCode) {
        if (StrUtil.isBlank(toEmail) || StrUtil.isBlank(inputCode)) {
            return "邮箱或验证码不能为空";
        }
        String email = toEmail.trim();
        RBucket<String> codeBucket = redissonClient.getBucket(EMAIL_CODE_KEY + email);
        String realCode = codeBucket.get();

        if (StrUtil.isBlank(realCode)) {
            return "验证码已失效，请重新获取";
        }
        if (!realCode.equalsIgnoreCase(inputCode.trim())) {
            return "验证码不正确";
        }

        // 校验成功后立即销毁验证码，防止二次重放
        codeBucket.delete();
        return null;
    }
}
