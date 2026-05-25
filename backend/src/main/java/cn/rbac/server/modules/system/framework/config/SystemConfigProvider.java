package cn.rbac.server.modules.system.framework.config;

import cn.rbac.server.framework.config.DynamicConfigProvider;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class SystemConfigProvider implements DynamicConfigProvider {

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Override
    public long getTokenExpirationMs() {
        return systemConfigHelper.getTokenExpirationMs();
    }

    @Override
    public boolean isConcurrentLogin() {
        return systemConfigHelper.isConcurrentLogin();
    }

    @Override
    public int getFileMaxSizeMb() {
        return systemConfigHelper.getFileMaxSizeMb();
    }

    @Override
    public String getFileAllowedExtensions() {
        return systemConfigHelper.getFileAllowedExtensions();
    }
}
