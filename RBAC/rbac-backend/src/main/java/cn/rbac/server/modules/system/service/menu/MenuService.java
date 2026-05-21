package cn.rbac.server.modules.system.service.menu;

import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;

import java.util.List;

public interface MenuService {

    List<MenuDO> listTree(String name, Integer status, Integer type);

    void deleteMenu(Long id);
}
