package cn.rbac.server.framework.security.api;

/**
 * 权限校验 SPI（由 system 模块实现，供 @PreAuthorize("@ss.hasPermission(...)") 使用）
 */
public interface PermissionApi {

    boolean hasPermission(String permission);

    boolean hasRole(String role);
}
