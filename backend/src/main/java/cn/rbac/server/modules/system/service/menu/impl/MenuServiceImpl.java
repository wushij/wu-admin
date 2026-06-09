package cn.rbac.server.modules.system.service.menu.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.menu.vo.MenuCreateReqVO;
import cn.rbac.server.modules.system.api.menu.vo.MenuUpdateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.dal.mysql.permission.MenuMapper;
import cn.rbac.server.modules.system.service.menu.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Resource
    private MenuMapper menuMapper;

    @Override
    public List<MenuDO> listTree(String name, Integer status, Integer type) {
        List<MenuDO> allMenus = menuMapper.selectList(new LambdaQueryWrapper<MenuDO>()
                .orderByAsc(MenuDO::getSort)
                .orderByAsc(MenuDO::getId));
        Map<Long, MenuDO> allById = allMenus.stream()
                .collect(Collectors.toMap(MenuDO::getId, m -> m, (a, b) -> a));

        Predicate<MenuDO> matchesFilter = menu -> {
            if (name != null && !name.isEmpty() && (menu.getName() == null || !menu.getName().contains(name))) {
                return false;
            }
            if (status != null && !Objects.equals(status, menu.getStatus())) {
                return false;
            }
            if (type != null && !Objects.equals(type, menu.getType())) {
                return false;
            }
            return !hasDisabledAncestor(menu.getParentId(), allById);
        };

        List<MenuDO> filtered = allMenus.stream().filter(matchesFilter).toList();
        return buildTree(filtered, allById);
    }

    @Override
    public List<MenuDO> listSimple() {
        return menuMapper.selectList(new LambdaQueryWrapper<MenuDO>()
                .orderByAsc(MenuDO::getSort)
                .orderByAsc(MenuDO::getId));
    }

    @Override
    public MenuDO getMenu(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public Long createMenu(MenuCreateReqVO reqVO) {
        MenuDO menu = new MenuDO();
        menu.setName(reqVO.getName());
        menu.setPermission(reqVO.getPermission());
        menu.setType(reqVO.getType());
        menu.setSort(reqVO.getSort() != null ? reqVO.getSort() : 0);
        menu.setParentId(reqVO.getParentId() != null ? reqVO.getParentId() : 0L);
        menu.setPath(reqVO.getPath());
        menu.setIcon(reqVO.getIcon());
        menu.setComponent(reqVO.getComponent());
        menu.setStatus(reqVO.getStatus() != null ? reqVO.getStatus() : 1);
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    public void updateMenu(MenuUpdateReqVO reqVO) {
        MenuDO menu = menuMapper.selectById(reqVO.getId());
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        menu.setName(reqVO.getName());
        menu.setPermission(reqVO.getPermission());
        menu.setType(reqVO.getType());
        menu.setSort(reqVO.getSort());
        menu.setParentId(reqVO.getParentId());
        menu.setPath(reqVO.getPath());
        menu.setIcon(reqVO.getIcon());
        menu.setComponent(reqVO.getComponent());
        menu.setStatus(reqVO.getStatus());
        menuMapper.updateById(menu);
    }

    @Override
    public void deleteMenu(Long id) {
        Long childCount = menuMapper.selectCount(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
    }

    @Override
    public PageResult<MenuDO> recyclePage(PageParam pageParam, String name, Integer status) {
        Page<MenuDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<MenuDO> deletedPage = (Page<MenuDO>) menuMapper.selectDeletedPage(page, name, status);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    public void restoreMenu(Long id) {
        int rows = menuMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站菜单不存在");
        }
    }

    @Override
    public void deletePermanent(Long id) {
        int rows = menuMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站菜单不存在");
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        MenuDO menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        menu.setStatus(status);
        menuMapper.updateById(menu);
    }

    private List<MenuDO> buildTree(List<MenuDO> menus, Map<Long, MenuDO> allById) {
        Set<Long> idSet = menus.stream().map(MenuDO::getId).collect(Collectors.toSet());
        Map<Long, List<MenuDO>> parentMap = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        for (MenuDO menu : menus) {
            attachVisibleChildren(menu, parentMap, allById);
        }
        List<MenuDO> roots = new ArrayList<>(parentMap.getOrDefault(0L, List.of()));
        for (MenuDO menu : menus) {
            Long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
            if (parentId != 0L && !idSet.contains(parentId)) {
                roots.add(menu);
            }
        }
        roots.sort(Comparator
                .comparing(MenuDO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuDO::getId, Comparator.nullsLast(Long::compareTo)));
        return roots;
    }

    private void attachVisibleChildren(MenuDO node, Map<Long, List<MenuDO>> parentMap,
                                       Map<Long, MenuDO> allById) {
        if (isDisabled(node)) {
            node.setChildren(null);
            return;
        }
        List<MenuDO> rawChildren = parentMap.get(node.getId());
        if (rawChildren == null || rawChildren.isEmpty()) {
            node.setChildren(null);
            return;
        }
        List<MenuDO> visibleChildren = new ArrayList<>();
        for (MenuDO child : rawChildren) {
            if (hasDisabledAncestor(child.getParentId(), allById)) {
                continue;
            }
            visibleChildren.add(child);
            attachVisibleChildren(child, parentMap, allById);
        }
        node.setChildren(visibleChildren.isEmpty() ? null : visibleChildren);
    }

    private boolean hasDisabledAncestor(Long parentId, Map<Long, MenuDO> allById) {
        Long pid = parentId == null ? 0L : parentId;
        while (pid != 0L) {
            MenuDO parent = allById.get(pid);
            if (parent == null) {
                break;
            }
            if (isDisabled(parent)) {
                return true;
            }
            pid = parent.getParentId() == null ? 0L : parent.getParentId();
        }
        return false;
    }

    private boolean isDisabled(MenuDO menu) {
        return menu != null && menu.getStatus() != null && menu.getStatus() == 0;
    }
}
