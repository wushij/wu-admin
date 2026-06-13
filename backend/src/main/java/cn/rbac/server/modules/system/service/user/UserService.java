package cn.rbac.server.modules.system.service.user;

import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.user.vo.AssignRoleReqVO;
import cn.rbac.server.modules.system.api.user.vo.UserCreateReqVO;
import cn.rbac.server.modules.system.api.user.vo.UserUpdateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;

import java.util.List;
import java.util.Set;

public interface UserService {

    List<UserDO> listAll();

    PageResult<UserDO> page(PageParam pageParam, String keyword, String username, String mobile,
                            Integer status, Long deptId, Long postId);

    UserDO getDetail(Long id);

    Long createUser(UserCreateReqVO reqVO);

    void updateUser(UserUpdateReqVO reqVO);

    void deleteUser(Long id);

    PageResult<UserDO> recyclePage(PageParam pageParam, String username,
                                    String mobile, Integer status, Long deptId);

    void restore(Long id);

    void deletePermanent(Long id);

    Set<Long> getRoleIds(Long userId);

    void assignRole(AssignRoleReqVO reqVO);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String rawPassword);

    void kickOut(Long userId);

    /** 解除登录失败临时锁定（Redis） */
    void unlockLogin(Long id);
}
