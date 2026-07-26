package com.admin.server.modules.trade.framework.sms;

import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsServiceFactory {

    private final SystemConfigHelper configHelper;
    private final List<SmsService> smsServices;

    private final Map<String, SmsService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (SmsService service : smsServices) {
            serviceMap.put(service.getProviderName(), service);
            log.info("注册短信服务: {}", service.getProviderName());
        }
        // 历史配置 provider=aliyun 统一走短信认证（SendSmsVerifyCode）
        SmsService dypns = serviceMap.get("aliyunAuth");
        if (dypns != null) {
            serviceMap.put("aliyun", dypns);
        }
    }

    public SmsService getService() {
        String provider = configHelper.getSmsProvider();
        SmsService service = serviceMap.get(provider);
        if (service == null) {
            log.warn("未找到短信服务商: {}, 使用控制台模式", provider);
            service = serviceMap.get("console");
        }
        return service;
    }

    public boolean sendCode(String phone, String code) {
        return getService().sendCode(phone, code);
    }

    public boolean sendCode(String phone, String code, String templateCode) {
        SmsService service = getService();
        if (templateCode != null && !templateCode.isBlank()) {
            return service.sendCodeWithTemplate(phone, code, templateCode.trim());
        }
        return service.sendCode(phone, code);
    }

    public boolean sendNotice(String phone, String title, String content) {
        return getService().sendNotice(phone, title, content);
    }
}
