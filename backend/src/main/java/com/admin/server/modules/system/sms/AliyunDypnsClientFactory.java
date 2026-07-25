package com.admin.server.modules.system.sms;

import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AliyunDypnsClientFactory {

    public Client createClient(SystemConfigHelper configHelper) throws Exception {
        String accessKeyId = configHelper.getSmsAccessKeyId();
        String accessKeySecret = configHelper.getSmsAccessKeySecret();
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint("dypnsapi.aliyuncs.com");
        return new Client(config);
    }

    public boolean isConfigured(SystemConfigHelper configHelper) {
        return StringUtils.hasText(configHelper.getSmsAccessKeyId())
                && StringUtils.hasText(configHelper.getSmsAccessKeySecret());
    }
}
