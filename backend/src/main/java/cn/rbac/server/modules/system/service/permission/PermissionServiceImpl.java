package cn.rbac.server.modules.system.service.permission;

import cn.rbac.server.modules.system.dal.dataobject.permission.*;
import cn.rbac.server.modules.system.dal.mysql.permission.*;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.annotation.Resource;
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
        roleMenuMapper.deleteByRoleId(roleId);
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
    public Set<Long> getRoleMenuIdsForAssign(Long roleId) {
        Set<Long> ids = getRoleMenuListByRoleId(roleId);
        if (CollUtil.isEmpty(ids)) {
            return ids;
        }
        List<MenuDO> menus = listMenusByIds(ids);
        Set<Long> parentIdsInAssigned = menus.stream()
                .map(MenuDO::getId)
                .filter(id -> menus.stream().anyMatch(m -> id.equals(m.getParentId())))
                .collect(Collectors.toSet());
        return ids.stream()
                .filter(id -> !parentIdsInAssigned.contains(id))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        if (matchPermissionDirect(userId, permission)) {
            return true;
        }
        // 仅有「xxx:query」时，允许访问「xxx:list」列表接口（只读）
        String queryAlias = toQueryAlias(permission);
        if (queryAlias != null && matchPermissionDirect(userId, queryAlias)) {
            return true;
        }
        // 有「xxx:upload」时，允许访问「xxx:list」（文件列表页）
        String uploadAlias = toUploadAlias(permission);
        return uploadAlias != null && matchPermissionDirect(userId, uploadAlias);
    }

    @Override
    public Set<String> getUserPermissionCodes(Long userId) {
        Set<String> owned = loadUserPermissionCodes(userId);
        Set<String> result = new HashSet<>(owned);
        for (String code : new ArrayList<>(owned)) {
            String listAlias = toListAlias(code);
            if (listAlias != null) {
                result.add(listAlias);
            }
        }
        return result;
    }

    private boolean matchPermissionDirect(Long userId, String permission) {
        Set<Long> roleIds = getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        for (Long roleId : roleIds) {
            for (Long menuId : getRoleMenuListByRoleId(roleId)) {
                MenuDO menu = menuMapper.selectById(menuId);
                if (menu != null && permission.equals(menu.getPermission())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 仅有 query 时，允许访问同资源的 list 接口（列表页只读） */
    private String toQueryAlias(String required) {
        if (required == null || !required.endsWith(":list")) {
            return null;
        }
        return required.substring(0, required.length() - 5) + ":query";
    }

    private String toUploadAlias(String required) {
        if (required == null || !required.endsWith(":list")) {
            return null;
        }
        return required.substring(0, required.length() - 5) + ":upload";
    }

    private String toListAlias(String owned) {
        if (owned == null || !owned.endsWith(":query")) {
            return null;
        }
        return owned.substring(0, owned.length() - 6) + ":list";
    }

    private Set<String> loadUserPermissionCodes(Long userId) {
        Set<Long> roleIds = getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        Set<Long> menuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            menuIds.addAll(getRoleMenuListByRoleId(roleId));
        }
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptySet();
        }
        Set<String> codes = new HashSet<>();
        for (MenuDO menu : listMenusByIds(menuIds)) {
            if (StringUtils.hasText(menu.getPermission())) {
                codes.add(menu.getPermission());
            }
        }
        return codes;
    }

    private List<MenuDO> listMenusByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return menuMapper.selectList(new LambdaQueryWrapper<MenuDO>().in(MenuDO::getId, ids));
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
        menuIds = expandMenuClosure(menuIds);
        List<MenuDO> menus = listMenusByIds(menuIds);
        menus.sort(Comparator
                .comparing(MenuDO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuDO::getId, Comparator.nullsLast(Long::compareTo)));
        return menus;
    }

    /**
     * 勾选按钮时自动补齐父级目录、菜单，否则侧栏不显示（前端只渲染 type≠3 的节点）
     */
    private Set<Long> expandMenuClosure(Set<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptySet();
        }
        Set<Long> result = new HashSet<>(menuIds);
        ArrayDeque<Long> queue = new ArrayDeque<>(menuIds);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            MenuDO menu = menuMapper.selectById(id);
            if (menu == null) {
                continue;
            }
            Long parentId = menu.getParentId();
            if (parentId != null && parentId > 0 && result.add(parentId)) {
                queue.add(parentId);
            }
        }
        return result;
    }
}
