package com.admin.server.modules.trade.service.email;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    @Resource
    private EmailLogService emailLogService;

    /**
     * 发送邮箱验证码（默认绑定场景）
     */
    public String sendEmailCode(String toEmail) {
        return sendEmailCode(toEmail, "bind");
    }

    /**
     * 发送邮箱验证码（支持指定业务场景：register / login / bind / resetPwd）
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
        String sceneTitle;
        String actionText;
        String normalizedScene = StrUtil.isBlank(scene) ? "bind" : scene.trim().toLowerCase();

        switch (normalizedScene) {
            case "register":
                subject = "【" + platformName + "】账号注册验证码";
                sceneTitle = "账号注册";
                actionText = "您正在注册【" + platformName + "】账号";
                break;
            case "login":
                subject = "【" + platformName + "】快捷登录验证码";
                sceneTitle = "快捷登录";
                actionText = "您正在登录【" + platformName + "】系统";
                break;
            case "resetpwd":
            case "modifypwd":
                subject = "【" + platformName + "】重置密码验证码";
                sceneTitle = "重置密码";
                actionText = "您正在申请重置/修改密码";
                break;
            case "bind":
            default:
                subject = "【" + platformName + "】绑定邮箱验证码";
                sceneTitle = "绑定邮箱";
                actionText = "您正在进行邮箱绑定/更换操作";
                break;
        }

        String plainText = "您好！" + actionText + "，您的验证码为：" + code + "，有效期 " + expireMin + " 分钟。如非本人操作请忽略。";
        String htmlContent = buildHtmlCodeTemplate(platformName, sceneTitle, actionText, code, expireMin);

        // 异步投递邮件
        CompletableFuture.runAsync(() -> {
            try {
                JavaMailSenderImpl sender = systemConfigHelper.buildDynamicMailSender();
                MimeMessage mimeMessage = sender.createMimeMessage();
                // 必须开启 multipart/alternative 模式（plainText + htmlContent），这是避免反垃圾邮件系统误判的关键 RFC 规范
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                String from = systemConfigHelper.getEmailUsername();
                String fromName = systemConfigHelper.getEmailFromName();
                if (StrUtil.isNotBlank(fromName)) {
                    helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
                } else {
                    helper.setFrom(from);
                }
                helper.setTo(email);
                helper.setSubject(subject);
                // 设置双格式（纯文本 fallback + HTML 渲染卡片），极大地降低 Spam 评分
                helper.setText(plainText, htmlContent);

                mimeMessage.setHeader("X-Priority", "1");
                mimeMessage.setHeader("X-MSMail-Priority", "High");
                mimeMessage.setHeader("Importance", "High");

                sender.send(mimeMessage);
                log.info("【{}】场景 HTML 邮件验证码已成功发送至 {}", normalizedScene, email);
                emailLogService.recordLog(email, subject, code, normalizedScene, systemConfigHelper.getEmailProvider(), true, "发送成功", null);
            } catch (Exception e) {
                log.error("发送【{}】场景邮件验证码至 {} 失败", normalizedScene, email, e);
                emailLogService.recordLog(email, subject, code, normalizedScene, systemConfigHelper.getEmailProvider(), false, e.getMessage(), null);
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
        String platformName = systemConfigHelper.getPlatformName();
        String subject = "【" + platformName + "】系统配置邮件服务测试";
        try {
            JavaMailSenderImpl sender = systemConfigHelper.buildDynamicMailSender();
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String from = systemConfigHelper.getEmailUsername();
            String fromName = systemConfigHelper.getEmailFromName();
            if (StrUtil.isNotBlank(fromName)) {
                helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
            } else {
                helper.setFrom(from);
            }
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String plainText = "您好！收到此邮件说明【" + platformName + "】系统的 SMTP 邮件服务配置正确，网络连通成功。发送时间：" + DateUtil.now();
            String htmlContent = buildHtmlTestTemplate(platformName);
            helper.setText(plainText, htmlContent);

            mimeMessage.setHeader("X-Priority", "1");
            mimeMessage.setHeader("X-MSMail-Priority", "High");

            sender.send(mimeMessage);
            log.info("测试邮件已成功发送至 {}", toEmail);
            emailLogService.recordLog(toEmail, subject, "连通性测试报文", "test", systemConfigHelper.getEmailProvider(), true, "测试成功", null);
            return null;
        } catch (Exception e) {
            log.error("测试邮件发送至 {} 失败", toEmail, e);
            emailLogService.recordLog(toEmail, subject, "连通性测试报文", "test", systemConfigHelper.getEmailProvider(), false, e.getMessage(), null);
            return e.getMessage();
        }
    }

    /**
     * 校验邮箱验证码
     */
    public String verifyEmailCode(String email, String code) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(code)) {
            return "邮箱和验证码不能为空";
        }
        RBucket<String> codeBucket = redissonClient.getBucket(EMAIL_CODE_KEY + email.trim());
        String savedCode = codeBucket.get();
        if (savedCode == null) {
            return "验证码已过期或不存在，请重新获取";
        }
        if (!savedCode.equalsIgnoreCase(code.trim())) {
            return "验证码错误";
        }
        codeBucket.delete();
        return null;
    }

    // ====== HTML 邮件模板生成 ======

    private String buildHtmlCodeTemplate(String platformName, String sceneTitle, String actionText, String code, int expireMin) {
        String logoHtml = getLogoHtml();

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>" +
                "<body style=\"margin: 0; padding: 30px 0; background-color: #f4f6f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;\">" +
                "<div style=\"max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #e5e7eb; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">" +
                "  <!-- Header -->" +
                "  <div style=\"background: #111827; padding: 24px 32px;\">" +
                "    <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +
                "      <tr>" +
                "        <td style=\"vertical-align: middle;\">" +
                "          " + logoHtml +
                "          <span style=\"color: #ffffff; font-size: 18px; font-weight: 700; vertical-align: middle; margin-left: 10px;\">" + escapeHtml(platformName) + "</span>" +
                "        </td>" +
                "        <td style=\"text-align: right; vertical-align: middle;\">" +
                "          <span style=\"background: rgba(255,255,255,0.15); color: #ffffff; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500;\">" + escapeHtml(sceneTitle) + "</span>" +
                "        </td>" +
                "      </tr>" +
                "    </table>" +
                "  </div>" +
                "  <!-- Body -->" +
                "  <div style=\"padding: 36px 32px;\">" +
                "    <h2 style=\"margin: 0 0 12px 0; color: #1f2937; font-size: 18px; font-weight: 600;\">您好！</h2>" +
                "    <p style=\"margin: 0 0 24px 0; color: #4b5563; font-size: 14px; line-height: 1.6;\">" + escapeHtml(actionText) + "，本次操作的验证码如下：</p>" +
                "    <!-- Code Box -->" +
                "    <div style=\"background: #f8fafc; border: 2px dashed #cbd5e1; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 24px;\">" +
                "      <div style=\"font-size: 36px; font-weight: 800; letter-spacing: 12px; color: #2563eb; font-family: Consolas, Monaco, monospace;\">" + escapeHtml(code) + "</div>" +
                "      <div style=\"margin-top: 10px; font-size: 13px; color: #64748b;\">⏱ 验证码有效期为 <strong>" + expireMin + " 分钟</strong>，请尽快完成验证</div>" +
                "    </div>" +
                "    <!-- Safety Warning -->" +
                "    <div style=\"background: #fffbeb; border-radius: 8px; padding: 14px 16px; border-left: 4px solid #f59e0b; color: #b45309; font-size: 13px; line-height: 1.5;\">" +
                "      🔒 <strong>安全提示：</strong>验证码包含敏感操作权限，请勿转发或提供给任何人。如非本人操作，请忽略此邮件。" +
                "    </div>" +
                "  </div>" +
                "  <!-- Footer -->" +
                "  <div style=\"background: #f8fafc; border-top: 1px solid #f1f5f9; padding: 20px 32px; text-align: center; color: #94a3b8; font-size: 12px; line-height: 1.6;\">" +
                "    <div>此邮件由 " + escapeHtml(platformName) + " 系统自动发出，请勿直接回复</div>" +
                "    <div style=\"margin-top: 4px;\">© " + DateUtil.thisYear() + " " + escapeHtml(platformName) + " · All rights reserved.</div>" +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildHtmlTestTemplate(String platformName) {
        String logoHtml = getLogoHtml();
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset=\"UTF-8\"></head>" +
                "<body style=\"margin: 0; padding: 30px 0; background-color: #f4f6f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;\">" +
                "<div style=\"max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #e5e7eb; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">" +
                "  <div style=\"background: #111827; padding: 28px 32px;\">" +
                "    " + logoHtml +
                "    <span style=\"color: #ffffff; font-size: 18px; font-weight: 700; vertical-align: middle; margin-left: 10px;\">" + escapeHtml(platformName) + "</span>" +
                "  </div>" +
                "  <div style=\"padding: 36px 32px;\">" +
                "    <h2 style=\"margin: 0 0 16px 0; color: #16a34a; font-size: 18px; font-weight: 600;\">✅ SMTP 邮件服务配置成功！</h2>" +
                "    <p style=\"color: #4b5563; font-size: 14px; line-height: 1.6;\">您好！收到此邮件说明【" + escapeHtml(platformName) + "】系统的 SMTP 邮件服务配置正确，网络连通正常。</p>" +
                "    <div style=\"margin-top: 20px; font-size: 13px; color: #64748b; background: #f8fafc; padding: 12px 16px; border-radius: 8px;\">发送时间：" + DateUtil.now() + "</div>" +
                "  </div>" +
                "  <div style=\"background: #f8fafc; border-top: 1px solid #f1f5f9; padding: 20px 32px; text-align: center; color: #94a3b8; font-size: 12px;\">" +
                "    © " + DateUtil.thisYear() + " " + escapeHtml(platformName) + " · All rights reserved." +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String getLogoHtml() {
        return "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"display: inline-block; vertical-align: middle; width: 32px; height: 32px; background: #010710; background: linear-gradient(135deg, #010710 0%, #374151 100%); border-radius: 8px; overflow: hidden;\">" +
                "  <tr>" +
                "    <td style=\"width: 32px; height: 32px; text-align: center; vertical-align: middle; padding: 0;\">" +
                "      <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" align=\"center\" style=\"margin: 0 auto;\">" +
                "        <tr>" +
                "          <td style=\"vertical-align: bottom; height: 16px; padding-right: 2px;\"><div style=\"width: 3px; height: 8px; background: #ffffff; border-radius: 1px;\"></div></td>" +
                "          <td style=\"vertical-align: bottom; height: 16px; padding-right: 2px;\"><div style=\"width: 3px; height: 14px; background: #ffffff; border-radius: 1px;\"></div></td>" +
                "          <td style=\"vertical-align: bottom; height: 16px;\"><div style=\"width: 3px; height: 6px; background: #ffffff; border-radius: 1px;\"></div></td>" +
                "        </tr>" +
                "      </table>" +
                "    </td>" +
                "  </tr>" +
                "</table>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
