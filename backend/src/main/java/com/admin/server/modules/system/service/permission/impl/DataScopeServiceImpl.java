package com.admin.server.modules.system.service.permission.impl;

import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.modules.system.dal.dataobject.permission.UserRoleDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.permission.RoleMapper;
import com.admin.server.modules.system.dal.mysql.permission.UserRoleMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.permission.DataScopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 组织（部门）数据权限实现
 * <p>数据范围取值：1 全部、2 自定义部门、3 本部门、4 本部门及以下、5 仅本人。
 * 多角色取并集；只要有一个角色是“全部”，即不受组织范围限制。</p>
 */
@Service
public class DataScopeServiceImpl implements DataScopeService {

    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_CUSTOM = 2;
    private static final int SCOPE_DEPT = 3;
    private static final int SCOPE_DEPT_AND_CHILD = 4;
    private static final int SCOPE_SELF = 5;

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private DeptMapper deptMapper;

    @Override
    public DataScopeContext getCurrentScope() {
        Long userId = SecurityUtils.getLoginUserId();
        if (userId == null) {
            // 没有登录上下文（异步线程、事件监听、定时任务）时不授予任何数据范围。
            // 系统任务如确需全量数据，应显式使用专用查询，而不是默认放开组织隔离。
            return new DataScopeContext(false, false, List.of(), null, null);
        }
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return new DataScopeContext(false, false, List.of(), userId, null);
        }
        List<UserRoleDO> links = userRoleMapper.selectListByUserId(userId);
        Set<Long> roleIds = new LinkedHashSet<>();
        for (UserRoleDO link : links) {
            if (link.getRoleId() != null) {
                roleIds.add(link.getRoleId());
            }
        }
        List<RoleDO> roles = roleIds.isEmpty() ? List.of() : roleMapper.selectByIds(roleIds);
        if (isSuperAdmin(userId, roles)) {
            return new DataScopeContext(true, false, List.of(), userId, user.getDeptId());
        }
        if (roles == null || roles.isEmpty()) {
            return new DataScopeContext(false, true, List.of(), userId, user.getDeptId());
        }

        boolean selfOnly = false;
        Set<Long> visibleDeptIds = new LinkedHashSet<>();
        for (RoleDO role : roles) {
            Integer scope = role.getDataScope();
            if (scope == null || scope == SCOPE_ALL) {
                return new DataScopeContext(true, false, List.of(), userId, user.getDeptId());
            }
            switch (scope) {
                case SCOPE_CUSTOM -> visibleDeptIds.addAll(parseDeptIds(role.getDataScopeDeptIds()));
                case SCOPE_DEPT -> {
                    if (user.getDeptId() != null) {
                        visibleDeptIds.add(user.getDeptId());
                    }
                }
                case SCOPE_DEPT_AND_CHILD -> {
                    if (user.getDeptId() != null) {
                        visibleDeptIds.addAll(selfAndDescendantIds(user.getDeptId()));
                    }
                }
                case SCOPE_SELF -> selfOnly = true;
                default -> {
                    // 未知取值按全部处理，保持与历史数据兼容
                    return new DataScopeContext(true, false, List.of(), userId, user.getDeptId());
                }
            }
        }
        return new DataScopeContext(false, selfOnly, new ArrayList<>(visibleDeptIds), userId, user.getDeptId());
    }

    @Override
    public Long getCurrentDeptId() {
        Long userId = SecurityUtils.getLoginUserId();
        if (userId == null) {
            return null;
        }
        UserDO user = userMapper.selectById(userId);
        return user != null ? user.getDeptId() : null;
    }

    @Override
    public List<Long> getVisibleDeptIds() {
        DataScopeContext context = getCurrentScope();
        if (context.all()) {
            return deptMapper.selectList(new LambdaQueryWrapper<DeptDO>()
                            .select(DeptDO::getId))
                    .stream().map(DeptDO::getId).toList();
        }
        Set<Long> ids = new LinkedHashSet<>(context.deptIds());
        // 仅本人范围也需要看到自己所在的部门节点
        if (context.selfOnly() && context.deptId() != null) {
            ids.add(context.deptId());
        }
        return new ArrayList<>(ids);
    }

    @Override
    public boolean isDeptVisible(Long deptId) {
        if (deptId == null) {
            return false;
        }
        DataScopeContext context = getCurrentScope();
        if (context.all()) {
            return true;
        }
        if (context.selfOnly() && Objects.equals(deptId, context.deptId())) {
            return true;
        }
        return context.deptIds().contains(deptId);
    }

    @Override
    public boolean isUserVisible(Long userId) {
        if (userId == null) {
            return false;
        }
        DataScopeContext context = getCurrentScope();
        if (context.all()) {
            return true;
        }
        if (context.userId() != null && context.userId().equals(userId)) {
            return true;
        }
        UserDO user = userMapper.selectById(userId);
        if (user == null || user.getDeptId() == null) {
            return false;
        }
        return context.deptIds().contains(user.getDeptId());
    }

    @Override
    public void applyUserScope(LambdaQueryWrapper<UserDO> wrapper) {
        DataScopeContext context = getCurrentScope();
        if (context.all()) {
            return;
        }
        List<Long> deptIds = context.deptIds();
        if (context.selfOnly()) {
            if (deptIds.isEmpty()) {
                wrapper.eq(UserDO::getId, context.userId());
            } else {
                wrapper.and(w -> w.in(UserDO::getDeptId, deptIds)
                        .or().eq(UserDO::getId, context.userId()));
            }
            return;
        }
        if (deptIds.isEmpty()) {
            if (context.userId() != null) {
                wrapper.eq(UserDO::getId, context.userId());
            } else {
                wrapper.eq(UserDO::getId, -1L);
            }
        } else {
            if (context.userId() != null) {
                wrapper.and(w -> w.in(UserDO::getDeptId, deptIds)
                        .or().eq(UserDO::getId, context.userId()));
            } else {
                wrapper.in(UserDO::getDeptId, deptIds);
            }
        }
    }

    @Override
    public void applyDeptScope(LambdaQueryWrapper<DeptDO> wrapper) {
        DataScopeContext context = getCurrentScope();
        if (context.all()) {
            return;
        }
        Set<Long> ids = new LinkedHashSet<>(context.deptIds());
        if (context.selfOnly() && context.deptId() != null) {
            ids.add(context.deptId());
        }
        if (ids.isEmpty()) {
            wrapper.eq(DeptDO::getId, -1L);
        } else {
            wrapper.in(DeptDO::getId, ids);
        }
    }

    @Override
    public <T> void applyDeptIdScope(LambdaQueryWrapper<T> wrapper, SFunction<T, ?> deptIdColumn) {
        applyDeptIdScope(wrapper, deptIdColumn, getCurrentScope());
    }

    @Override
    public <T> void applyDeptIdScope(LambdaQueryWrapper<T> wrapper, SFunction<T, ?> deptIdColumn,
                                     DataScopeContext context) {
        if (context == null || context.all()) {
            return;
        }
        Set<Long> ids = new LinkedHashSet<>(context.deptIds());
        if (context.selfOnly() && context.deptId() != null) {
            ids.add(context.deptId());
        }
        if (ids.isEmpty()) {
            wrapper.eq(deptIdColumn, -1L);
        } else {
            wrapper.in(deptIdColumn, ids);
        }
    }

    @Override
    public <T> void applyCreatorScope(LambdaQueryWrapper<T> wrapper, SFunction<T, ?> creatorUserIdColumn) {
        DataScopeContext context = getCurrentScope();
        if (context.all()) {
            return;
        }
        Set<Long> ids = new LinkedHashSet<>(context.deptIds());
        if (context.selfOnly()) {
            if (ids.isEmpty()) {
                wrapper.eq(creatorUserIdColumn, context.userId() == null ? -1L : context.userId());
                return;
            }
            String subQuery = "SELECT id FROM sys_user WHERE deleted = 0 AND dept_id IN ("
                    + joinIds(ids) + ")";
            wrapper.and(w -> w.eq(creatorUserIdColumn, context.userId())
                    .or().inSql(creatorUserIdColumn, subQuery));
            return;
        }
        if (ids.isEmpty()) {
            wrapper.eq(creatorUserIdColumn, -1L);
            return;
        }
        wrapper.inSql(creatorUserIdColumn,
                "SELECT id FROM sys_user WHERE deleted = 0 AND dept_id IN (" + joinIds(ids) + ")");
    }

    @Override
    public String visibleUserIdsSql() {
        DataScopeContext context = getCurrentScope();
        if (context.all()) {
            return null;
        }
        if (context.selfOnly()) {
            return context.userId() == null
                    ? "SELECT id FROM sys_user WHERE 1 = 0"
                    : "SELECT id FROM sys_user WHERE deleted = 0 AND id = " + context.userId();
        }
        if (context.deptIds().isEmpty()) {
            return "SELECT id FROM sys_user WHERE 1 = 0";
        }
        return "SELECT id FROM sys_user WHERE deleted = 0 AND dept_id IN (" + joinIds(context.deptIds()) + ")";
    }

    private boolean isSuperAdmin(Long userId, List<RoleDO> roles) {
        if (Long.valueOf(1L).equals(userId)) {
            return true;
        }
        if (roles == null) {
            return false;
        }
        return roles.stream().anyMatch(role -> role.getCode() != null
                && "super_admin".equalsIgnoreCase(role.getCode()));
    }

    private List<Long> parseDeptIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                long id = Long.parseLong(part.trim());
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
                // 忽略非法部门 ID，避免脏数据阻断整个查询
            }
        }
        return ids;
    }

    /**
     * 计算部门自身及全部下级部门
     */
    private Collection<Long> selfAndDescendantIds(Long deptId) {
        List<DeptDO> all = deptMapper.selectList(new LambdaQueryWrapper<DeptDO>()
                .select(DeptDO::getId, DeptDO::getParentId));
        List<Long> ids = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        Set<Long> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || !visited.add(current)) {
                continue;
            }
            ids.add(current);
            for (DeptDO dept : all) {
                if (Objects.equals(dept.getParentId(), current) && dept.getId() != null) {
                    queue.add(dept.getId());
                }
            }
        }
        return ids;
    }

    private String joinIds(Collection<Long> ids) {
        return ids.stream().filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }
}
