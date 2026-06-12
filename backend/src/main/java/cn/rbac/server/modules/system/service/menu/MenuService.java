package cn.rbac.server.modules.system.service.menu;

import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.menu.vo.MenuCreateReqVO;
import cn.rbac.server.modules.system.api.menu.vo.MenuUpdateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;

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
