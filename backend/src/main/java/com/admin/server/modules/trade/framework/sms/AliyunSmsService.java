package com.admin.server.modules.trade.framework.sms;

import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.trade.service.sms.SmsLogService;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 旧版 Dysms SendSms，已弃用；阿里云统一使用 {@link AliyunDypnsSmsService}。
 */
@Slf4j
// @Service 已停用，避免与 aliyunAuth 冲突
@RequiredArgsConstructor
public class AliyunSmsService implements SmsService {

    private final SystemConfigHelper configHelper;
    private final SmsLogService smsLogService;

    @Override
    public boolean sendCode(String phone, String code) {
        return sendCodeWithTemplate(phone, code, configHelper.getSmsTemplateVerifyCode());
    }

    @Override
    public boolean sendCodeWithTemplate(String phone, String code, String templateCode) {
        String accessKeyId = configHelper.getSmsAccessKeyId();
        String accessKeySecret = configHelper.getSmsAccessKeySecret();
        String signName = configHelper.getSmsSignName();
        String templateCodeVal = templateCode;

        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            log.warn("阿里云短信配置不完整，使用控制台打印模式");
            log.info("【短信验证码 - 阿里云(未配置)】phone={}, code={}", phone, code);
            smsLogService.logVerifyCode(phone, code, "console", true, "控制台打印模式", null);
            return true;
        }

        String bizId = null;
        String resultMsg = null;
        boolean success = false;

        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint("dysmsapi.aliyuncs.com");
            Client client = new Client(config);

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCodeVal)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            SendSmsResponse response = client.sendSms(request);
            String respCode = response.getBody().getCode();
            bizId = response.getBody().getBizId();
            resultMsg = response.getBody().getMessage();

            if ("OK".equals(respCode)) {
                log.info("阿里云短信发送成功: phone={}, bizId={}", phone, bizId);
                success = true;
            } else {
                log.error("阿里云短信发送失败: code={}, message={}", respCode, resultMsg);
            }
        } catch (Exception e) {
            log.error("阿里云短信发送异常", e);
            resultMsg = e.getMessage();
        }

        smsLogService.logVerifyCode(phone, code, getProviderName(), success, resultMsg, bizId);
        return success;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean sendNotice(String phone, String title, String content) {
        String templateCode = configHelper.getSmsTemplateNotice();
        if (!StringUtils.hasText(templateCode)) {
            log.warn("通知短信模板未配置，使用控制台打印");
            smsLogService.logVerifyCode(phone, truncate(content, 20), "aliyun", true, "模板未配置", null);
            return true;
        }
        String accessKeyId = configHelper.getSmsAccessKeyId();
        String accessKeySecret = configHelper.getSmsAccessKeySecret();
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            return false;
        }
        try {
            String param = "{\"content\":\"" + escapeJson(truncate(content, 100)) + "\"}";
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint("dysmsapi.aliyuncs.com");
            Client client = new Client(config);
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(configHelper.getSmsSignName())
                    .setTemplateCode(templateCode)
                    .setTemplateParam(param);
            SendSmsResponse response = client.sendSms(request);
            boolean success = "OK".equals(response.getBody().getCode());
            smsLogService.logVerifyCode(phone, truncate(content, 20), "aliyun", success,
                    response.getBody().getMessage(), response.getBody().getBizId());
            return success;
        } catch (Exception e) {
            log.error("阿里云通知短信发送失败", e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "aliyun";
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    private static String escapeJson(String text) {
        return text.replace("\"", "\\\"");
    }
}
