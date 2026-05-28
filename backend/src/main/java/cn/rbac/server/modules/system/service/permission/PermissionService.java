package cn.rbac.server.modules.system.service.permission;

import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import java.util.List;
import java.util.Set;

public interface PermissionService {
    // 用户角色
    void assignUserRole(Long userId, Set<Long> roleIds);
    Set<Long> getUserRoleIdListByUserId(Long userId);
    
    // 角色菜单
    void assignRoleMenu(Long roleId, Set<Long> menuIds);
    Set<Long> getRoleMenuListByRoleId(Long roleId);
    /** 分配权限弹窗回显：仅返回叶子节点，避免父节点导致树组件整棵子树显示为全选 */
    Set<Long> getRoleMenuIdsForAssign(Long roleId);
    
    // 权限校验
    boolean hasPermission(Long userId, String permission);
    boolean hasRole(Long userId, String role);
    
    // 获取用户菜单
    List<MenuDO> getUserMenuList(Long userId);

    /** 用户拥有的权限标识（含 query→list 只读推导，供前端 v-permission） */
    Set<String> getUserPermissionCodes(Long userId);
}
