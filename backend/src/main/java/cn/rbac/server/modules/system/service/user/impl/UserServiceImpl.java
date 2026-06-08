package cn.rbac.server.modules.system.service.user.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.modules.system.api.user.vo.AssignRoleReqVO;
import cn.rbac.server.modules.system.api.user.vo.UserCreateReqVO;
import cn.rbac.server.modules.system.api.user.vo.UserUpdateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.post.PostDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserPostDO;
import cn.rbac.server.modules.system.dal.mysql.dept.DeptMapper;
import cn.rbac.server.modules.system.dal.mysql.post.PostMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserPostMapper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.rbac.server.modules.system.service.user.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    /** /list 接口最大返回条数（供下拉选择，非管理列表） */
    private static final int LIST_ALL_MAX = 2000;

    @Resource
    private UserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private TokenService tokenService;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public List<UserDO> listAll() {
        return userMapper.selectListForOptions(LIST_ALL_MAX);
    }

    @Override
    public PageResult<UserDO> page(PageParam pageParam, String username, String mobile,
                                    Integer status, Long deptId, Long postId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(UserDO::getUsername, username);
        }
        if (mobile != null && !mobile.isEmpty()) {
            wrapper.like(UserDO::getMobile, mobile);
        }
        if (status != null) {
            wrapper.eq(UserDO::getStatus, status);
        } else {
            wrapper.in(UserDO::getStatus, 0, 1);
        }
        if (deptId != null) {
            wrapper.eq(UserDO::getDeptId, deptId);
        }
        if (postId != null) {
            List<Long> userIds = userPostMapper.selectUserIdsByPostId(postId);
            if (userIds.isEmpty()) {
                wrapper.eq(UserDO::getId, -1L);
            } else {
                wrapper.in(UserDO::getId, userIds);
            }
        }
        Page<UserDO> page = userMapper.selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        List<UserDO> users = page.getRecords();
        fillUserDisplayFields(users);
        return PageResult.of(users, page.getTotal());
    }

    @Override
    public UserDO getDetail(Long id) {
        UserDO user = userMapper.selectById(id);
        if (user != null) {
            fillUserDisplayFields(Collections.singletonList(user));
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateReqVO reqVO) {
        UserDO user = new UserDO();
        user.setUsername(reqVO.getUsername());
        user.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        user.setNickname(reqVO.getNickname());
        user.setMobile(reqVO.getMobile());
        user.setEmail(reqVO.getEmail());
        user.setStatus(1);
        user.setDeptId(reqVO.getDeptId());
        userMapper.insert(user);
        if (reqVO.getRoleId() != null) {
            permissionService.assignUserRole(user.getId(), Collections.singleton(reqVO.getRoleId()));
        }
        saveUserPosts(user.getId(), reqVO.getPostIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateReqVO reqVO) {
        UserDO user = userMapper.selectById(reqVO.getId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setNickname(reqVO.getNickname());
        user.setMobile(reqVO.getMobile());
        user.setEmail(reqVO.getEmail());
        user.setStatus(reqVO.getStatus());
        user.setDeptId(reqVO.getDeptId());
        userMapper.updateById(user);
        if (reqVO.getRoleId() != null) {
            permissionService.assignUserRole(user.getId(), Collections.singleton(reqVO.getRoleId()));
        }
        saveUserPosts(user.getId(), reqVO.getPostIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        userPostMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    @Override
    public PageResult<UserDO> recyclePage(PageParam pageParam, String username,
                                            String mobile, Integer status, Long deptId) {
        Page<UserDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<UserDO> deletedPage = (Page<UserDO>) userMapper.selectDeletedPage(page, username, mobile, status, deptId);
        List<UserDO> users = deletedPage.getRecords();
        if (!users.isEmpty()) {
            Set<Long> deptIds = users.stream().map(UserDO::getDeptId).filter(id -> id != null).collect(Collectors.toSet());
            if (!deptIds.isEmpty()) {
                List<DeptDO> depts = deptMapper.selectByIds(deptIds);
                if (depts != null) {
                    Map<Long, String> deptNameMap = depts.stream().collect(Collectors.toMap(DeptDO::getId, DeptDO::getName));
                    users.forEach(user -> {
                        if (user.getDeptId() != null) {
                            user.setDeptName(deptNameMap.get(user.getDeptId()));
                        }
                    });
                }
            }
            users.forEach(user -> user.setRoleIds(permissionService.getUserRoleIdListByUserId(user.getId())));
        }
        return PageResult.of(users, deletedPage.getTotal());
    }

    @Override
    public void restore(Long id) {
        int rows = userMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站用户不存在");
        }
    }

    @Override
    public void deletePermanent(Long id) {
        int rows = userMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站用户不存在");
        }
    }

    @Override
    public Set<Long> getRoleIds(Long userId) {
        return permissionService.getUserRoleIdListByUserId(userId);
    }

    @Override
    public void assignRole(AssignRoleReqVO reqVO) {
        permissionService.assignUserRole(reqVO.getUserId(), reqVO.getRoleIds());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String rawPassword) {
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
    }

    @Override
    public void kickOut(Long userId) {
        tokenService.removeToken(userId);
    }

    // ---- 私有方法 ----

    private void fillUserDisplayFields(List<UserDO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> deptIds = users.stream().map(UserDO::getDeptId).filter(id -> id != null).collect(Collectors.toSet());
        if (!deptIds.isEmpty()) {
            List<DeptDO> depts = deptMapper.selectByIds(deptIds);
            if (depts != null) {
                Map<Long, String> deptNameMap = depts.stream().collect(Collectors.toMap(DeptDO::getId, DeptDO::getName));
                users.forEach(user -> {
                    if (user.getDeptId() != null) {
                        user.setDeptName(deptNameMap.get(user.getDeptId()));
                    }
                });
            }
        }
        Set<Long> userIdSet = users.stream().map(UserDO::getId).collect(Collectors.toSet());
        List<UserPostDO> pageLinks = userPostMapper.selectByUserIds(userIdSet);
        Map<Long, List<Long>> userPostMap = pageLinks.stream()
                .collect(Collectors.groupingBy(UserPostDO::getUserId,
                        Collectors.mapping(UserPostDO::getPostId, Collectors.toList())));
        Set<Long> postIds = pageLinks.stream().map(UserPostDO::getPostId).collect(Collectors.toSet());
        Map<Long, String> postNameMap;
        if (!postIds.isEmpty()) {
            List<PostDO> postList = postMapper.selectByIds(postIds);
            postNameMap = postList != null
                    ? postList.stream().collect(Collectors.toMap(PostDO::getId, PostDO::getPostName))
                    : Collections.emptyMap();
        } else {
            postNameMap = Collections.emptyMap();
        }
        Map<Long, Set<Long>> userRoleMap = permissionService.getUserRoleIdsMapByUserIds(userIdSet);
        users.forEach(user -> {
            user.setRoleIds(userRoleMap.getOrDefault(user.getId(), Collections.emptySet()));
            List<Long> pids = userPostMap.getOrDefault(user.getId(), Collections.emptyList());
            user.setPostIds(pids);
            if (!pids.isEmpty()) {
                user.setPostNames(pids.stream()
                        .map(postNameMap::get)
                        .filter(name -> name != null)
                        .collect(Collectors.joining("、")));
            }
        });
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        userPostMapper.deleteByUserId(userId);
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        for (Long postId : postIds) {
            if (postId == null) {
                continue;
            }
            UserPostDO link = new UserPostDO();
            link.setUserId(userId);
            link.setPostId(postId);
            userPostMapper.insert(link);
        }
    }
}
