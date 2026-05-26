package cn.rbac.server.modules.system.service.dept;

import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;

import java.util.List;

public interface DeptService {

    List<DeptDO> tree(String name, Integer status);

    List<DeptDO> listAll();

    DeptDO getById(Long id);

    void create(DeptDO dept);

    void update(DeptDO dept);

    void delete(Long id);

    void move(Long id, Long parentId, Integer sort);
}
