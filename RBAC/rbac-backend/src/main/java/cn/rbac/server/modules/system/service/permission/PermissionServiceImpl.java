package cn.rbac.server.modules.system.service.permission;

import cn.rbac.server.modules.system.dal.dataobject.permission.*;
import cn.rbac.server.modules.system.dal.mysql.permission.*;
import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private MenuMapper menuMapper;

    @Override
    @Transactional
    public void assignUserRole(Long userId, Set<Long> roleIds) {
        // 删除原有关联
        userRoleMapper.deleteByUserId(userId);
        // 新增关联
        if (CollUtil.isNotEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                UserRoleDO userRole = new UserRoleDO();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        List<UserRoleDO> list = userRoleMapper.selectListByUserId(userId);
        return list.stream().map(UserRoleDO::getRoleId).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void assignRoleMenu(Long roleId, Set<Long> menuIds) {
        // 删除原有关联
        roleMenuMapper.deleteByRoleId(roleId);
        // 新增关联
        if (CollUtil.isNotEmpty(menuIds)) {
            for (Long menuId : menuIds) {
                RoleMenuDO roleMenu = new RoleMenuDO();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }
    }

    @Override
    public Set<Long> getRoleMenuListByRoleId(Long roleId) {
        List<RoleMenuDO> list = roleMenuMapper.selectListByRoleId(roleId);
        return list.stream().map(RoleMenuDO::getMenuId).collect(Collectors.toSet());
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        Set<Long> roleIds = getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        for (Long roleId : roleIds) {
            Set<Long> menuIds = getRoleMenuListByRoleId(roleId);
            for (Long menuId : menuIds) {
                MenuDO menu = menuMapper.selectById(menuId);
                if (menu != null && permission.equals(menu.getPermission())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasRole(Long userId, String role) {
        Set<Long> roleIds = getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        for (Long roleId : roleIds) {
            RoleDO roleDO = roleMapper.selectById(roleId);
            if (roleDO != null && role.equals(roleDO.getCode())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<MenuDO> getUserMenuList(Long userId) {
        Set<Long> roleIds = getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        Set<Long> menuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            menuIds.addAll(getRoleMenuListByRoleId(roleId));
        }
        if (CollUtil.isEmpty(menuIds)) {
            return new ArrayList<>();
        }
        return menuMapper.selectBatchIds(menuIds);
    }
}
