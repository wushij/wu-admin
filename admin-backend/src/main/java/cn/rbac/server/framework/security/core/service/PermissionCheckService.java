package cn.rbac.server.framework.security.core.service;

import cn.rbac.server.modules.system.service.permission.PermissionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 自定义权限校验服务（用于 @PreAuthorize 注解）
 */
@Service("ss")
public class PermissionCheckService {

    @Resource
    private PermissionService permissionService;

    /**
     * 校验用户是否有指定权限
     * @param permission 权限标识
     * @return 是否有权限
     */
    public boolean hasPermission(String permission) {
        Long userId = SecurityUtils.getLoginUserId();
        if (userId == null) {
            return false;
        }
        // 超级管理员拥有所有权限
        if (permissionService.hasRole(userId, "super_admin")) {
            return true;
        }
        return permissionService.hasPermission(userId, permission);
    }

    /**
     * 校验用户是否有指定角色
     * @param role 角色编码
     * @return 是否有角色
     */
    public boolean hasRole(String role) {
        Long userId = SecurityUtils.getLoginUserId();
        if (userId == null) {
            return false;
        }
        return permissionService.hasRole(userId, role);
    }
}
