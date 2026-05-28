package cn.rbac.server.modules.system.framework.security;

import cn.rbac.server.framework.security.api.PermissionApi;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 权限校验（Spring EL bean 名 {@code ss}，兼容既有 @PreAuthorize）
 */
@Service("ss")
public class SystemPermissionService implements PermissionApi {

    @Resource
    private PermissionService permissionService;

    @Override
    public boolean hasPermission(String permission) {
        Long userId = SecurityUtils.getLoginUserId();
        if (userId == null) {
            return false;
        }
        if (permissionService.hasRole(userId, "super_admin")) {
            return true;
        }
        return permissionService.hasPermission(userId, permission);
    }

    @Override
    public boolean hasRead(String permission) {
        if (hasPermission(permission)) {
            return true;
        }
        if (permission != null && permission.endsWith(":list")) {
            String prefix = permission.substring(0, permission.length() - 5);
            return hasPermission(prefix + "query") || hasPermission(prefix + "upload");
        }
        return false;
    }

    @Override
    public boolean hasRole(String role) {
        Long userId = SecurityUtils.getLoginUserId();
        if (userId == null) {
            return false;
        }
        return permissionService.hasRole(userId, role);
    }
}
