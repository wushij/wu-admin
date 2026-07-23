package com.admin.server.framework.security.api;

/**
 * 权限校验 SPI（由 system 模块实现，供 @PreAuthorize("@ss.hasPermission(...)") 使用）
 */
public interface PermissionApi {

    boolean hasPermission(String permission);

    /** 列表/详情等只读接口：有 list、query 或 upload（文件）任一即可 */
    boolean hasRead(String permission);

    /** 回收中心：查看汇总与各模块回收站列表 */
    boolean hasRecycleRead();

    /** 回收中心：恢复（含各模块 delete 权限的兼容） */
    boolean hasRecycleRestore(String moduleDeletePerm);

    /** 回收中心：彻底删除（含各模块 delete 权限的兼容） */
    boolean hasRecycleDelete(String moduleDeletePerm);

    boolean hasRole(String role);
}
