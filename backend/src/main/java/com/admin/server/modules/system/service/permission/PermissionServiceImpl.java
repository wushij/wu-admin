package com.admin.server.modules.system.service.permission;

import com.admin.server.modules.system.dal.dataobject.permission.*;
import com.admin.server.modules.system.dal.mysql.permission.*;
import com.admin.server.modules.system.service.permission.event.UserPermissionChangedEvent;
import cn.hutool.core.collection.CollUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRole(Long userId, Set<Long> roleIds) {
        // 删除原有关联
        userRoleMapper.deleteByUserId(userId);
        // 新增关联（批量）
        if (CollUtil.isNotEmpty(roleIds)) {
            List<UserRoleDO> list = new ArrayList<>(roleIds.size());
            for (Long roleId : roleIds) {
                UserRoleDO userRole = new UserRoleDO();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                list.add(userRole);
            }
            userRoleMapper.insertBatch(list);
        }
        // 通知下游模块失效该用户的权限上下文缓存（如 AI 助手用户上下文）
        eventPublisher.publishEvent(new UserPermissionChangedEvent(userId));
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        // 权限查询以 userId / roleId 为锚点，直接查询关联表
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
            // 勾选按钮时同步补齐父级页面/目录，与 getUserMenuList 一致，避免侧栏能进但接口 403
            menuIds = expandMenuClosure(menuIds);
            List<RoleMenuDO> list = new ArrayList<>(menuIds.size());
            for (Long menuId : menuIds) {
                RoleMenuDO roleMenu = new RoleMenuDO();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                list.add(roleMenu);
            }
            roleMenuMapper.insertBatch(list);
        }
        // 角色菜单变更影响该角色下所有用户，广播全量失效
        eventPublisher.publishEvent(new UserPermissionChangedEvent(null));
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
        // 只查勾选到的这批菜单（数量可控），不与全表比较
        List<MenuDO> menus = menuMapper.selectByIds(ids);
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
        // 一次 JOIN 取回该用户全部权限码，避免每次校验都回查整张菜单表
        Set<String> owned = new HashSet<>(menuMapper.selectPermissionCodesByUserId(userId));
        if (owned.isEmpty()) {
            return false;
        }
        if (owned.contains(permission)) {
            return true;
        }
        // 仅有「xxx:query」时，允许访问「xxx:list」列表接口（只读）
        String queryAlias = toQueryAlias(permission);
        if (queryAlias != null && owned.contains(queryAlias)) {
            return true;
        }
        // 有「xxx:upload」时，允许访问「xxx:list」（文件列表页）
        String uploadAlias = toUploadAlias(permission);
        return uploadAlias != null && owned.contains(uploadAlias);
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
        if (userId == null) {
            return Collections.emptySet();
        }
        List<String> codes = menuMapper.selectPermissionCodesByUserId(userId);
        return new HashSet<>(codes);
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
        if (userId == null) {
            return new ArrayList<>();
        }
        // 一次 JOIN 取回该用户被授权的全部菜单（含目录与按钮）
        List<MenuDO> granted = menuMapper.selectMenusByUserId(userId);
        if (CollUtil.isEmpty(granted)) {
            return new ArrayList<>();
        }
        // 侧栏只需要 type!=3 的目录/页面；按钮权限由权限码集合单独判定
        List<MenuDO> visible = granted.stream()
                .filter(m -> m.getType() == null || m.getType() != 3)
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .collect(Collectors.toList());
        if (visible.isEmpty()) {
            return new ArrayList<>();
        }

        // 判断祖先是否被停用需要完整菜单树（授权集合里可能不含未授权的中间层级）
        Map<Long, MenuDO> allById = new HashMap<>();
        for (MenuDO menu : menuMapper.selectList(null)) {
            allById.put(menu.getId(), menu);
        }
        visible.removeIf(m -> hasDisabledAncestor(m.getParentId(), allById));

        visible.sort(Comparator
                .comparing(MenuDO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuDO::getId, Comparator.nullsLast(Long::compareTo)));
        return visible;
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

    @Override
    public List<Long> getUserIdsByRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Collections.emptyList();
        }
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getCode, roleCode)
                .eq(RoleDO::getDeleted, 0)
                .last("LIMIT 1"));
        if (role == null) {
            return Collections.emptyList();
        }
        List<UserRoleDO> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getRoleId, role.getId()));
        return links.stream().map(UserRoleDO::getUserId).distinct().collect(Collectors.toList());
    }
}
