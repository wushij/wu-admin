package cn.rbac.server.modules.system.sms;

import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.sms.SmsLogService;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunDypnsSmsService implements SmsService {

    static final String SMS_OUT_ID_KEY = "sms:outId:";

    private final SystemConfigHelper configHelper;
    private final SmsLogService smsLogService;
    private final AliyunDypnsClientFactory clientFactory;
    private final RedissonClient redissonClient;

    @Override
    public boolean sendCode(String phone, String code) {
        return sendCodeWithTemplate(phone, code, configHelper.getSmsTemplateVerifyCode());
    }

    @Override
    public boolean sendCodeWithTemplate(String phone, String code, String templateCode) {
        if (!clientFactory.isConfigured(configHelper)) {
            log.warn("阿里云短信认证配置不完整，使用控制台打印模式");
            log.info("【短信验证码 - 阿里云短信认证(未配置)】phone={}, template={}, code={}", phone, templateCode, code);
            smsLogService.logVerifyCode(phone, code, "console", true, "控制台打印模式", null);
            return true;
        }

        String signName = configHelper.getSmsSignName();
        int expireMinutes = configHelper.getSmsCodeExpireMinutes();

        String bizId = null;
        String resultMsg = null;
        boolean success = false;

        try {
            Client client = clientFactory.createClient(configHelper);
            String templateParam = String.format("{\"code\":\"%s\",\"min\":\"%d\"}", code, expireMinutes);
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam(templateParam)
                    .setPhoneNumber(phone);

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            SendSmsVerifyCodeResponseBody body = response.getBody();
            if (body != null && body.getModel() != null) {
                bizId = body.getModel().getBizId();
                cacheOutId(phone, body.getModel().getOutId(), expireMinutes);
            }
            if (body != null && (Boolean.TRUE.equals(body.getSuccess()) || "OK".equalsIgnoreCase(body.getCode()))) {
                log.info("阿里云短信认证发送成功: phone={}, template={}, bizId={}", phone, templateCode, bizId);
                success = true;
                resultMsg = body.getMessage();
            } else if (body != null) {
                resultMsg = body.getMessage();
                log.error("阿里云短信认证发送失败: template={}, code={}, message={}", templateCode, body.getCode(), resultMsg);
            } else {
                resultMsg = "响应为空";
            }
        } catch (TeaException e) {
            resultMsg = e.getMessage();
            log.error("阿里云短信认证发送异常: template={}, {}", templateCode, resultMsg, e);
        } catch (Exception e) {
            resultMsg = e.getMessage();
            log.error("阿里云短信认证发送异常: template={}", templateCode, e);
        }

        smsLogService.logVerifyCode(phone, code, getProviderName(), success, resultMsg, bizId);
        return success;
    }

    private void cacheOutId(String phone, String outId, int expireMinutes) {
        if (!StringUtils.hasText(outId)) {
            return;
        }
        RBucket<String> bucket = redissonClient.getBucket(SMS_OUT_ID_KEY + phone);
        bucket.set(outId, expireMinutes, TimeUnit.MINUTES);
    }

    @Override
    public boolean sendNotice(String phone, String title, String content) {
        log.warn("阿里云短信认证暂不支持通知短信，phone={}, content={}", phone, content);
        return false;
    }

    @Override
    public String getProviderName() {
        return "aliyunAuth";
    }
}
