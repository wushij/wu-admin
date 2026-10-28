package com.admin.server.modules.system.service.user;

import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.modules.system.api.user.vo.AssignRoleReqVO;
import com.admin.server.modules.system.api.user.vo.UserCreateReqVO;
import com.admin.server.modules.system.api.user.vo.UserUpdateReqVO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserService {

    List<UserDO> listAll();

    /** 获得所有状态启用的用户列表 */
    List<UserDO> getEnabledUserList();

    /** 根据用户ID集合批量获取用户列表 */
    List<UserDO> getUserList(Collection<Long> ids);

    /** 根据部门ID集合获取用户列表 */
    List<UserDO> getUserListByDeptIds(Collection<Long> deptIds);

    PageResult<UserDO> page(PageParam pageParam, String keyword, String username, String mobile,
                            Integer status, Long deptId, Long postId, Boolean loginLocked);

    UserDO getDetail(Long id);

    /**
     * 读取用户展示信息（昵称、头像、用户名），不受当前用户组织数据权限约束。
     *
     * <p>仅用于「把他人名字/头像展示给当前用户」的场景，例如消息发送者、公告发布人。
     * 管理端按 ID 查看用户资料仍须使用 {@link #getDetail(Long)}——它带数据范围校验，
     * 普通用户越权查询会得到 403。</p>
     */
    UserDO getUserForDisplay(Long id);

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

    /** 处理用户注册审核结果（通过或驳回） */
    void handleRegisterAudit(Long userId, boolean approved);

    /** 根据关键词模糊匹配用户（用户名或昵称），返回未删除的用户列表 */
    List<UserDO> searchUsersByKeyword(String keyword);

    /** 根据用户名或昵称集合批量匹配用户 */
    List<UserDO> listUsersByUsernamesOrNicknames(Collection<String> names);
}
