package com.admin.server.modules.trade.framework.sms;

import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 阿里云短信认证验证码核验（Dypnsapi CheckSmsVerifyCode）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunDypnsSmsVerifyService {

    private final SystemConfigHelper configHelper;
    private final AliyunDypnsClientFactory clientFactory;
    private final RedissonClient redissonClient;

    public boolean verifyCode(String phone, String verifyCode) {
        if (!clientFactory.isConfigured(configHelper)) {
            log.warn("阿里云短信认证未配置，跳过云端核验");
            return false;
        }
        try {
            Client client = clientFactory.createClient(configHelper);
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                    .setSchemeName(configHelper.getSmsSchemeName())
                    .setCountryCode("86")
                    .setPhoneNumber(phone)
                    .setVerifyCode(verifyCode)
                    .setCaseAuthPolicy(1L);

            String outId = getCachedOutId(phone);
            if (StringUtils.hasText(outId)) {
                request.setOutId(outId);
            }

            CheckSmsVerifyCodeResponse response = client.checkSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            CheckSmsVerifyCodeResponseBody body = response.getBody();
            if (body == null) {
                return false;
            }
            if (Boolean.TRUE.equals(body.getSuccess()) || "OK".equalsIgnoreCase(body.getCode())) {
                if (body.getModel() != null && "PASS".equalsIgnoreCase(body.getModel().getVerifyResult())) {
                    return true;
                }
            }
            log.warn("阿里云短信认证核验未通过: phone={}, message={}", phone, body.getMessage());
            return false;
        } catch (TeaException e) {
            log.error("阿里云短信认证核验异常: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("阿里云短信认证核验异常", e);
            return false;
        }
    }

    private String getCachedOutId(String phone) {
        RBucket<String> bucket = redissonClient.getBucket(AliyunDypnsSmsService.SMS_OUT_ID_KEY + phone);
        return bucket.get();
    }
}
