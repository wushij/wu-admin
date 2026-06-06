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
        // 工作台：前端固定路由，凡已登录用户均可查看统计（无需单独菜单权限）
        if ("dashboard:stats:view".equals(permission)) {
            return SecurityUtils.getLoginUserId() != null;
        }
        if (permission != null && permission.endsWith(":list")) {
            String prefix = permission.substring(0, permission.length() - 5);
            if (hasPermission(prefix + ":query") || hasPermission(prefix + ":upload")) {
                return true;
            }
            // 用户管理页岗位下拉：有用户读/写权限即可拉取启用岗位列表
            if ("system:post".equals(prefix)) {
                return hasPermission("system:user:query")
                        || hasPermission("system:user:create")
                        || hasPermission("system:user:update");
            }
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
