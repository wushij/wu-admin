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
    @Transactional(rollbackFor = Exception.class)
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
    public Map<Long, Set<Long>> getUserRoleIdsMapByUserIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<UserRoleDO> allLinks = userRoleMapper.selectByUserIds(userIds);
        return allLinks.stream().collect(Collectors.groupingBy(
                UserRoleDO::getUserId,
                Collectors.mapping(UserRoleDO::getRoleId, Collectors.toSet())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        List<MenuDO> menus = listMenusByIds(ids, true);
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
        // 批量收集所有角色关联的菜单ID，一次查询代替 N*M 次逐条查询
        Set<Long> allMenuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            allMenuIds.addAll(getRoleMenuListByRoleId(roleId));
        }
        if (CollUtil.isEmpty(allMenuIds)) {
            return false;
        }
        List<MenuDO> menus = listMenusByIds(allMenuIds);
        return menus.stream().anyMatch(menu -> permission.equals(menu.getPermission()));
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
        return listMenusByIds(ids, false);
    }

    private List<MenuDO> listMenusByIds(Collection<Long> ids, boolean includeDisabled) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MenuDO> wrapper = new LambdaQueryWrapper<MenuDO>().in(MenuDO::getId, ids);
        if (!includeDisabled) {
            wrapper.eq(MenuDO::getStatus, 1);
        }
        return menuMapper.selectList(wrapper);
    }

    @Override
    public boolean hasRole(Long userId, String role) {
        Set<Long> roleIds = getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        // 批量查询角色，代替逐条 selectById
        List<RoleDO> roles = roleMapper.selectByIds(roleIds);
        if (CollUtil.isEmpty(roles)) {
            return false;
        }
        return roles.stream().anyMatch(roleDO -> role.equals(roleDO.getCode()));
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
        List<MenuDO> allMenus = menuMapper.selectList(new LambdaQueryWrapper<MenuDO>()
                .orderByAsc(MenuDO::getSort)
                .orderByAsc(MenuDO::getId));
        Map<Long, MenuDO> allById = allMenus.stream()
                .collect(Collectors.toMap(MenuDO::getId, m -> m, (a, b) -> a));
        List<MenuDO> menus = listMenusByIds(menuIds).stream()
                .filter(m -> !hasDisabledAncestor(m.getParentId(), allById))
                .collect(Collectors.toList());
        menus.sort(Comparator
                .comparing(MenuDO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuDO::getId, Comparator.nullsLast(Long::compareTo)));
        return menus;
    }

    private boolean hasDisabledAncestor(Long parentId, Map<Long, MenuDO> allById) {
        Long pid = parentId == null ? 0L : parentId;
        while (pid != 0L) {
            MenuDO parent = allById.get(pid);
            if (parent == null) {
                break;
            }
            if (parent.getStatus() != null && parent.getStatus() == 0) {
                return true;
            }
            pid = parent.getParentId() == null ? 0L : parent.getParentId();
        }
        return false;
    }

    /**
     * 勾选按钮时自动补齐父级目录、菜单，否则侧栏不显示（前端只渲染 type≠3 的节点）
     * 优化：批量加载菜单树后在内存中遍历，避免 BFS 逐条查库
     */
    private Set<Long> expandMenuClosure(Set<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptySet();
        }
        // 先加载全量菜单（菜单表通常不大），在内存中构建 parent 映射
        List<MenuDO> allMenus = menuMapper.selectList(null);
        Map<Long, Long> parentMap = new HashMap<>();
        for (MenuDO menu : allMenus) {
            if (menu.getParentId() != null && menu.getParentId() > 0) {
                parentMap.put(menu.getId(), menu.getParentId());
            }
        }
        Set<Long> result = new HashSet<>(menuIds);
        ArrayDeque<Long> queue = new ArrayDeque<>(menuIds);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            Long parentId = parentMap.get(id);
            if (parentId != null && parentId > 0 && result.add(parentId)) {
                queue.add(parentId);
            }
        }
        return result;
    }
}
