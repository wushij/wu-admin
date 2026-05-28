package cn.rbac.server.framework.config;

/**
 * 动态运行时配置（由业务模块实现，framework 仅依赖此接口）
 */
public interface DynamicConfigProvider {

    long getTokenExpirationMs();

    int getFileMaxSizeMb();

    String getFileAllowedExtensions();
}
