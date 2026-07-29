package com.admin.server.modules.system.service.permission.event;

/**
 * 用户权限上下文变更事件（角色分配 / 角色菜单调整 / 用户资料与部门变更时发布）
 * <p>
 * system 模块发布、下游模块（如 ai）监听做缓存失效，保持 system 不反向依赖下游。
 * userId 为 null 表示影响面为全体用户（如角色菜单重新授权）。
 * </p>
 */
public class UserPermissionChangedEvent {

    /** 受影响的用户ID；null 表示全体用户 */
    private final Long userId;

    public UserPermissionChangedEvent(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    /** 是否影响全体用户 */
    public boolean affectsAllUsers() {
        return userId == null;
    }
}
