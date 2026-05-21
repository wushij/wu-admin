package cn.rbac.server.modules.system.service.menu.impl;

import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.dal.mysql.permission.MenuMapper;
import cn.rbac.server.modules.system.service.menu.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Resource
    private MenuMapper menuMapper;

    @Override
    public List<MenuDO> listTree(String name, Integer status, Integer type) {
        LambdaQueryWrapper<MenuDO> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(MenuDO::getName, name);
        }
        if (status != null) {
            wrapper.eq(MenuDO::getStatus, status);
        }
        if (type != null) {
            wrapper.eq(MenuDO::getType, type);
        }
        wrapper.orderByAsc(MenuDO::getSort).orderByAsc(MenuDO::getId);
        List<MenuDO> list = menuMapper.selectList(wrapper);
        return buildTree(list);
    }

    @Override
    public void deleteMenu(Long id) {
        Long childCount = menuMapper.selectCount(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
    }

    private List<MenuDO> buildTree(List<MenuDO> menus) {
        Map<Long, List<MenuDO>> parentMap = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        menus.forEach(m -> {
            List<MenuDO> children = parentMap.get(m.getId());
            m.setChildren(children == null || children.isEmpty() ? null : children);
        });
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }
}
