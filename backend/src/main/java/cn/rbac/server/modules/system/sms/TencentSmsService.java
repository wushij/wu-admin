package cn.rbac.server.modules.system.sms;

import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.sms.SmsLogService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TencentSmsService implements SmsService {

    private final SystemConfigHelper configHelper;
    private final SmsLogService smsLogService;

    @Override
    public boolean sendCode(String phone, String code) {
        return sendCodeWithTemplate(phone, code, configHelper.getSmsTemplateVerifyCode());
    }

    @Override
    public boolean sendCodeWithTemplate(String phone, String code, String templateId) {
        String secretId = configHelper.getSmsAccessKeyId();
        String secretKey = configHelper.getSmsAccessKeySecret();
        String appId = configHelper.getSmsTencentAppId();
        String signName = configHelper.getSmsSignName();

        if (!StringUtils.hasText(secretId) || !StringUtils.hasText(secretKey)) {
            log.warn("腾讯云短信配置不完整，使用控制台打印模式");
            log.info("【短信验证码 - 腾讯云(未配置)】phone={}, template={}, code={}", phone, templateId, code);
            smsLogService.logVerifyCode(phone, code, "console", true, "控制台打印模式", null);
            return true;
        }

        String bizId = null;
        String resultMsg = null;
        boolean success = false;

        try {
            Credential cred = new Credential(secretId, secretKey);
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            httpProfile.setReqMethod("POST");
            httpProfile.setConnTimeout(60);

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            clientProfile.setSignMethod("HmacSHA256");

            SmsClient client = new SmsClient(cred, "ap-guangzhou", clientProfile);

            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(appId);
            request.setSignName(signName);
            request.setTemplateId(templateId);
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            request.setTemplateParamSet(new String[]{code});

            SendSmsResponse response = client.SendSms(request);
            SendStatus[] sendStatusSet = response.getSendStatusSet();

            if (sendStatusSet != null && sendStatusSet.length > 0) {
                SendStatus status = sendStatusSet[0];
                bizId = status.getSerialNo();
                resultMsg = status.getMessage();
                if ("Ok".equals(status.getCode())) {
                    log.info("腾讯云短信发送成功: phone={}, template={}, serialNo={}", phone, templateId, bizId);
                    success = true;
                } else {
                    log.error("腾讯云短信发送失败: template={}, code={}, message={}", templateId, status.getCode(), resultMsg);
                }
            } else {
                resultMsg = "响应结果为空";
                log.error("腾讯云短信发送失败: {}", resultMsg);
            }
        } catch (Exception e) {
            log.error("腾讯云短信发送异常: template={}", templateId, e);
            resultMsg = e.getMessage();
        }

        smsLogService.logVerifyCode(phone, code, getProviderName(), success, resultMsg, bizId);
        return success;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean sendNotice(String phone, String title, String content) {
        String templateId = configHelper.getSmsTemplateNotice();
        if (!StringUtils.hasText(templateId)) {
            log.info("【短信通知 - 腾讯云】模板未配置, phone={}, content={}", phone, content);
            return true;
        }
        try {
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(configHelper.getSmsTencentAppId());
            request.setSignName(configHelper.getSmsSignName());
            request.setTemplateId(templateId);
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            request.setTemplateParamSet(new String[]{truncate(content, 100)});

            Credential cred = new Credential(configHelper.getSmsAccessKeyId(), configHelper.getSmsAccessKeySecret());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(cred, "ap-guangzhou", clientProfile);

            SendSmsResponse response = client.SendSms(request);
            if (response.getSendStatusSet() != null && response.getSendStatusSet().length > 0) {
                return "Ok".equals(response.getSendStatusSet()[0].getCode());
            }
        } catch (Exception e) {
            log.error("腾讯云通知短信发送失败", e);
        }
        return false;
    }

    @Override
    public String getProviderName() {
        return "tencent";
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
