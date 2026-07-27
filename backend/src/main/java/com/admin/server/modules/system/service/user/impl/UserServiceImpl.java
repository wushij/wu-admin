package com.admin.server.modules.system.service.user.impl;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.modules.system.enums.ErrorCodeConstants;
import com.admin.server.framework.security.core.service.TokenService;
import com.admin.server.modules.system.api.user.vo.AssignRoleReqVO;
import com.admin.server.modules.system.api.user.vo.UserCreateReqVO;
import com.admin.server.modules.system.api.user.vo.UserUpdateReqVO;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.dal.dataobject.post.PostDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.dataobject.user.UserPostDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.admin.server.modules.system.dal.mysql.post.PostMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.dal.mysql.user.UserPostMapper;
import com.admin.server.modules.system.service.auth.LoginLockService;
import com.admin.server.modules.system.service.auth.vo.LoginLockStatusVO;
import com.admin.server.modules.system.service.dept.DeptService;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.modules.system.service.user.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.admin.server.modules.infra.framework.operlog.OperLogDiffUtils;
import com.admin.server.modules.infra.framework.operlog.OperLogContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    @Resource
    private DeptService deptService;
    @Resource
    private LoginLockService loginLockService;
    @Resource
    private LoginLogMapper loginLogMapper;

    @Override
    public List<UserDO> listAll() {
        return userMapper.selectListForOptions(LIST_ALL_MAX);
    }

    @Override
    public PageResult<UserDO> page(PageParam pageParam, String keyword, String username, String mobile,
                                    Integer status, Long deptId, Long postId, Boolean loginLocked) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(UserDO::getUsername, keyword)
                    .or().like(UserDO::getNickname, keyword)
                    .or().like(UserDO::getMobile, keyword));
        } else {
            if (username != null && !username.isEmpty()) {
                wrapper.like(UserDO::getUsername, username);
            }
            if (mobile != null && !mobile.isEmpty()) {
                wrapper.like(UserDO::getMobile, mobile);
            }
        }
        if (Boolean.TRUE.equals(loginLocked)) {
            Set<String> lockedUsernames = collectLockedUsernames();
            if (lockedUsernames.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L);
            }
            wrapper.in(UserDO::getUsername, lockedUsernames);
        }
        if (status != null) {
            wrapper.eq(UserDO::getStatus, status);
        } else {
            wrapper.in(UserDO::getStatus, 0, 1);
        }
        if (deptId != null) {
            List<Long> deptIds = deptService.listSelfAndDescendantIds(deptId);
            if (deptIds.isEmpty()) {
                wrapper.eq(UserDO::getDeptId, -1L);
            } else {
                wrapper.in(UserDO::getDeptId, deptIds);
            }
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
        fillLoginLockFields(users);
        return PageResult.of(users, page.getTotal());
    }

    @Override
    public UserDO getDetail(Long id) {
        UserDO user = userMapper.selectById(id);
        if (user != null) {
            fillUserDisplayFields(Collections.singletonList(user));
            fillLoginLockFields(Collections.singletonList(user));
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
            throw new BusinessException(ErrorCodeConstants.USER_NOT_EXISTS);
        }
        
        // 计算变更明细并记录操作日志
        List<String> diffItems = OperLogDiffUtils.diff(user, reqVO);
        if (!diffItems.isEmpty()) {
            OperLogContext.setDiffItems(diffItems);
            OperLogContext.setAction("修改用户「" + user.getUsername() + "」: " + String.join("；", diffItems));
        }

        String previousNickname = user.getNickname();
        user.setNickname(reqVO.getNickname());
        user.setMobile(reqVO.getMobile());
        user.setEmail(reqVO.getEmail());
        user.setStatus(reqVO.getStatus());
        user.setDeptId(reqVO.getDeptId());
        userMapper.updateById(user);
        deptService.syncLeaderByUserId(user.getId(), previousNickname, user.getNickname());
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
            throw new BusinessException(ErrorCodeConstants.USER_NOT_EXISTS);
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String rawPassword) {
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.USER_NOT_EXISTS);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
    }

    @Override
    public void kickOut(Long userId) {
        tokenService.removeToken(userId);
    }

    @Override
    public void unlockLogin(Long id) {
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.USER_NOT_EXISTS);
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BusinessException(400, "用户名为空，无法解除锁定");
        }
        loginLockService.unlockUser(user.getUsername());
        String recentIp = findRecentLoginIp(user.getUsername());
        if (recentIp != null) {
            loginLockService.unlockIp(recentIp);
        }
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

    private void fillLoginLockFields(List<UserDO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<String> usernames = users.stream()
                .map(UserDO::getUsername)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        Map<String, LoginLockStatusVO> statusMap = loginLockService.getUserLockStatusMap(usernames);
        Map<String, String> recentIpMap = findRecentLoginIpMap(usernames);
        Map<String, LoginLockStatusVO> ipStatusMap = loginLockService.getIpLockStatusMap(recentIpMap.values());
        users.forEach(user -> {
            if (user.getUsername() == null) {
                return;
            }
            String username = user.getUsername().trim();
            LoginLockStatusVO status = statusMap.get(username);
            if (status == null) {
                user.setLoginLocked(false);
                user.setLoginLockRemainSeconds(0L);
                user.setLoginFailCount(0);
            } else {
                user.setLoginLocked(status.isLocked());
                user.setLoginLockRemainSeconds(status.getRemainSeconds());
                user.setLoginFailCount(status.getFailCount());
            }
            String recentIp = recentIpMap.get(username);
            user.setLoginRecentIp(recentIp);
            if (recentIp == null) {
                user.setLoginIpLocked(false);
                user.setLoginIpLockRemainSeconds(0L);
                user.setLoginIpFailCount(0);
                return;
            }
            LoginLockStatusVO ipStatus = ipStatusMap.get(recentIp);
            if (ipStatus == null) {
                user.setLoginIpLocked(false);
                user.setLoginIpLockRemainSeconds(0L);
                user.setLoginIpFailCount(0);
                return;
            }
            user.setLoginIpLocked(ipStatus.isLocked());
            user.setLoginIpLockRemainSeconds(ipStatus.getRemainSeconds());
            user.setLoginIpFailCount(ipStatus.getFailCount());
        });
    }

    private Set<String> collectLockedUsernames() {
        Set<String> lockedUsernames = new HashSet<>(loginLockService.listLockedUsernames());
        lockedUsernames.addAll(findUsernamesByLockedIps(loginLockService.listLockedIps()));
        return lockedUsernames;
    }

    private Set<String> findUsernamesByLockedIps(Set<String> lockedIps) {
        if (lockedIps == null || lockedIps.isEmpty()) {
            return Collections.emptySet();
        }
        List<LoginLogDO> logs = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLogDO>()
                .in(LoginLogDO::getIpaddr, lockedIps)
                .isNotNull(LoginLogDO::getUsername)
                .ne(LoginLogDO::getUsername, "")
                .orderByDesc(LoginLogDO::getLoginTime));
        Set<String> result = new HashSet<>();
        Map<String, String> recentIpByUser = new HashMap<>();
        for (LoginLogDO log : logs) {
            if (log.getUsername() == null || log.getIpaddr() == null) {
                continue;
            }
            recentIpByUser.putIfAbsent(log.getUsername().trim(), log.getIpaddr().trim());
        }
        for (Map.Entry<String, String> entry : recentIpByUser.entrySet()) {
            if (lockedIps.contains(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private Map<String, String> findRecentLoginIpMap(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyMap();
        }
        List<LoginLogDO> logs = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLogDO>()
                .in(LoginLogDO::getUsername, usernames)
                .isNotNull(LoginLogDO::getIpaddr)
                .ne(LoginLogDO::getIpaddr, "")
                .orderByDesc(LoginLogDO::getLoginTime));
        Map<String, String> map = new HashMap<>();
        for (LoginLogDO log : logs) {
            if (log.getUsername() == null || log.getIpaddr() == null) {
                continue;
            }
            map.putIfAbsent(log.getUsername().trim(), log.getIpaddr().trim());
        }
        return map;
    }

    private String findRecentLoginIp(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return findRecentLoginIpMap(List.of(username.trim())).get(username.trim());
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