package com.admin.server.modules.system.service.role;

import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.api.role.vo.RoleCreateReqVO;
import com.admin.server.modules.system.api.role.vo.RoleUpdateReqVO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;

import java.util.List;
import java.util.Set;

public interface RoleService {

    List<RoleDO> list(String name, Integer status);

    PageResult<RoleDO> page(PageParam pageParam);

    RoleDO getById(Long id);

    Long create(RoleCreateReqVO reqVO);

    void update(RoleUpdateReqVO reqVO);

    void delete(Long id);

    PageResult<RoleDO> recyclePage(PageParam pageParam, String name, Integer status);

    void restore(Long id);

    void deletePermanent(Long id);

    Set<Long> getMenuIdsForAssign(Long roleId);

    void assignMenu(Long roleId, Set<Long> menuIds);

    void updateStatus(Long id, Integer status);
}
