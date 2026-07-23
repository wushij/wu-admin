package com.admin.server.modules.system.service.dept;

import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;

import java.util.List;

public interface DeptService {

    List<DeptDO> tree(String name, Integer status);

    List<DeptDO> listAll();

    DeptDO getById(Long id);

    void create(DeptDO dept);

    void update(DeptDO dept);

    void delete(Long id);

    void move(Long id, Long parentId, Integer sort);

    void updateStatus(Long id, Integer status);

    PageResult<DeptDO> recyclePage(PageParam pageParam, String name, Integer status);

    void restore(Long id);

    void deletePermanent(Long id);

    /** 用户昵称变更时，同步部门负责人展示名 */
    void syncLeaderDisplayName(String previousName, String newName);

    /** 用户资料变更时，同步其作为负责人的部门展示名 */
    void syncLeaderByUserId(Long userId, String previousName, String newNickname);

    /** 部门自身及全部下级部门 ID（用户按部门筛选时包含子部门） */
    List<Long> listSelfAndDescendantIds(Long deptId);
}
