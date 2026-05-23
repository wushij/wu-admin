package cn.rbac.server.framework.security.api;

/**
 * 权限校验 SPI（由 system 模块实现，供 @PreAuthorize("@ss.hasPermission(...)") 使用）
 */
public interface PermissionApi {

    boolean hasPermission(String permission);

    /** 列表/详情等只读接口：有 list、query 或 upload（文件）任一即可 */
    boolean hasRead(String permission);

    boolean hasRole(String role);
}
