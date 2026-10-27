package com.admin.server.modules.system.framework.config;

import com.admin.server.framework.config.DynamicConfigProvider;
import com.admin.server.framework.storage.StorageConfig;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component("dynamicConfigProvider")
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

    @Override
    public StorageConfig getStorageConfig() {
        return systemConfigHelper.getStorageConfig();
    }

    @Override
    public int getAuthPerIpMinute() {
        return systemConfigHelper.getAuthPerIpMinute();
    }

    @Override
    public int getFilesPerIpMinute() {
        return systemConfigHelper.getFilesPerIpMinute();
    }

    @Override
    public int getApiPerIpMinute() {
        return systemConfigHelper.getApiPerIpMinute();
    }

    @Override
    public boolean isTimestampEnabled() {
        return systemConfigHelper.isTimestampEnabled();
    }

    @Override
    public boolean isNonceEnabled() {
        return systemConfigHelper.isNonceEnabled();
    }

    @Override
    public boolean isSm2SignEffective() {
        return systemConfigHelper.isSm2SignEffective();
    }

    @Override
    public boolean isSm4EncryptEffective() {
        return systemConfigHelper.isSm4EncryptEffective();
    }

    @Override
    public String getSm3SignKey() {
        return systemConfigHelper.getSm3SignKey();
    }

    @Override
    public String getSm4SecretKey() {
        return systemConfigHelper.getSm4SecretKey();
    }

    @Override
    public boolean isRiskEnabled() {
        return systemConfigHelper.isRiskEnabled();
    }

    @Override
    public boolean isRiskBehaviorEnabled() {
        return systemConfigHelper.isRiskBehaviorEnabled();
    }

    @Override
    public int getRiskExportThreshold() {
        return systemConfigHelper.getRiskExportThreshold();
    }

    @Override
    public int getRiskPageScanThreshold() {
        return systemConfigHelper.getRiskPageScanThreshold();
    }

    @Override
    public boolean isRiskChallengeEnabled() {
        return systemConfigHelper.isRiskChallengeEnabled();
    }
}
