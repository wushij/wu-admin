package com.admin.server.framework.config;

import com.admin.server.framework.storage.StorageConfig;

/**
 * 动态运行时配置（由业务模块实现，framework 仅依赖此接口）
 */
public interface DynamicConfigProvider {

    long getTokenExpirationMs();

    /** Sa-Token is-concurrent：true 允许多端同时在线，false 新登录踢掉旧会话 */
    boolean isConcurrentLogin();

    int getFileMaxSizeMb();

    String getFileAllowedExtensions();

    /** 存储引擎配置 */
    StorageConfig getStorageConfig();

    /** 频率限制配置 */
    int getAuthPerIpMinute();
    int getFilesPerIpMinute();
    int getApiPerIpMinute();

    /** 接口安全与国密签名加密配置 */
    boolean isTimestampEnabled();
    boolean isNonceEnabled();
    boolean isSm2SignEffective();
    boolean isSm4EncryptEffective();
    String getSm3SignKey();
    String getSm4SecretKey();

    /** 风控门禁配置 */
    boolean isRiskEnabled();
    boolean isRiskBehaviorEnabled();
    int getRiskExportThreshold();
    int getRiskPageScanThreshold();
    boolean isRiskChallengeEnabled();
}
