package cn.rbac.server.framework.config;

/**
 * 动态运行时配置（由业务模块实现，framework 仅依赖此接口）
 */
public interface DynamicConfigProvider {

    long getTokenExpirationMs();

    /** Sa-Token is-concurrent：true 允许多端同时在线，false 新登录踢掉旧会话 */
    boolean isConcurrentLogin();

    int getFileMaxSizeMb();

    String getFileAllowedExtensions();
}
