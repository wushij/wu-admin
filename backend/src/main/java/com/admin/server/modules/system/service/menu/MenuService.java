package com.admin.server.modules.system.service.menu;

import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.modules.system.api.menu.vo.MenuCreateReqVO;
import com.admin.server.modules.system.api.menu.vo.MenuUpdateReqVO;
import com.admin.server.modules.system.dal.dataobject.permission.MenuDO;

import java.util.List;

public interface MenuService {

    List<MenuDO> listTree(String name, Integer status, Integer type);

    List<MenuDO> listSimple();

    MenuDO getMenu(Long id);

    Long createMenu(MenuCreateReqVO reqVO);

    void updateMenu(MenuUpdateReqVO reqVO);

    void deleteMenu(Long id);

    PageResult<MenuDO> recyclePage(PageParam pageParam, String name, Integer status);

    void restoreMenu(Long id);

    void deletePermanent(Long id);

    void updateStatus(Long id, Integer status);
}
