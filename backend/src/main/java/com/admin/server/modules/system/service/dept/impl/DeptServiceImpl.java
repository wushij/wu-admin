package com.admin.server.modules.system.service.dept.impl;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.modules.system.enums.ErrorCodeConstants;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.dept.DeptService;
import com.admin.server.modules.system.service.permission.DataScopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.admin.server.modules.infra.framework.operlog.OperLogDiffUtils;
import com.admin.server.modules.infra.framework.operlog.OperLogContext;

import jakarta.annotation.Resource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, DeptDO> implements DeptService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private DataScopeService dataScopeService;

    @Override
    public List<DeptDO> tree(String name, Integer status) {
        LambdaQueryWrapper<DeptDO> wrapper = new LambdaQueryWrapper<>();
        // 组织隔离：仅展示当前角色数据范围内的部门
        dataScopeService.applyDeptScope(wrapper);
        wrapper.like(StringUtils.hasText(name), DeptDO::getName, name)
                .eq(status != null, DeptDO::getStatus, status)
                .orderByAsc(DeptDO::getSort);
        List<DeptDO> list = list(wrapper);
        fillUserCount(list);
        fillLeaderNames(list);
        return buildTree(list);
    }

    @Override
    public List<DeptDO> listAll() {
        LambdaQueryWrapper<DeptDO> wrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyDeptScope(wrapper);
        return list(wrapper.orderByAsc(DeptDO::getSort));
    }

    @Override
    public DeptDO getById(Long id) {
        LambdaQueryWrapper<DeptDO> wrapper = new LambdaQueryWrapper<DeptDO>().eq(DeptDO::getId, id);
        dataScopeService.applyDeptScope(wrapper);
        DeptDO dept = getOne(wrapper);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在或无权查看");
        }
        fillLeaderNames(List.of(dept));
        return dept;
    }

    @Override
    public void create(DeptDO dept) {
        Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
        requireVisibleParent(parentId);
        dept.setParentId(parentId);
        if (parentId == 0L) {
            dept.setAncestors("0");
        } else {
            DeptDO parent = super.getById(parentId);
            if (parent == null) {
                throw new BusinessException(404, "父部门不存在");
            }
            dept.setAncestors(parent.getAncestors() + "," + parentId);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        resolveLeaderBinding(dept);
        save(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeptDO dept) {
        DeptDO exist = requireVisibleDept(dept.getId());

        // 计算变更明细并记录操作日志
        List<String> diffItems = OperLogDiffUtils.diff(exist, dept);
        if (!diffItems.isEmpty()) {
            OperLogContext.setDiffItems(diffItems);
            OperLogContext.setAction("修改部门「" + exist.getName() + "」: " + String.join("；", diffItems));
        }

        Long parentId = dept.getParentId() == null ? exist.getParentId() : dept.getParentId();
        if (parentId == null) {
            parentId = 0L;
        }
        if (dept.getId().equals(parentId)) {
            throw new BusinessException("上级部门不能选择自己");
        }
        if (!parentId.equals(exist.getParentId() == null ? 0L : exist.getParentId())) {
            requireVisibleParent(parentId);
            if (parentId == 0L) {
                dept.setAncestors("0");
            } else {
                DeptDO parent = super.getById(parentId);
                if (parent == null) {
                    throw new BusinessException(404, "父部门不存在");
                }
                if (parent.getAncestors() != null && parent.getAncestors().contains("," + dept.getId())) {
                    throw new BusinessException("不能移动到子部门下");
                }
                dept.setAncestors(parent.getAncestors() + "," + parentId);
            }
            dept.setParentId(parentId);
            resolveLeaderBinding(dept);
            updateById(dept);
            updateChildAncestors(dept);
        } else {
            resolveLeaderBinding(dept);
            updateById(dept);
        }
    }

    @Override
    public void delete(Long id) {
        requireVisibleDept(id);
        long childCount = count(new LambdaQueryWrapper<DeptDO>().eq(DeptDO::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCodeConstants.DEPT_HAS_CHILDREN);
        }
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<UserDO>().eq(UserDO::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException("部门下仍有 " + userCount + " 名用户，无法删除");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long parentId, Integer sort) {
        DeptDO dept = requireVisibleDept(id);
        parentId = parentId == null ? 0L : parentId;
        if (id.equals(parentId)) {
            throw new BusinessException("上级部门不能选择自己");
        }
        requireVisibleParent(parentId);
        if (parentId > 0) {
            DeptDO parent = super.getById(parentId);
            if (parent == null) {
                throw new BusinessException(404, "父部门不存在");
            }
            if (parent.getAncestors() != null && parent.getAncestors().contains("," + id)) {
                throw new BusinessException("不能移动到子部门下");
            }
            dept.setAncestors(parent.getAncestors() + "," + parentId);
        } else {
            dept.setAncestors("0");
        }
        dept.setParentId(parentId);
        if (sort != null) {
            dept.setSort(sort);
        }
        updateById(dept);
        updateChildAncestors(dept);
    }

    private DeptDO requireVisibleDept(Long id) {
        DeptDO dept = super.getById(id);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        if (!dataScopeService.isDeptVisible(id)) {
            throw new BusinessException(403, "无权操作该部门");
        }
        return dept;
    }

    private void requireVisibleParent(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return;
        }
        if (!dataScopeService.isDeptVisible(parentId)) {
            throw new BusinessException(403, "无权操作该上级部门");
        }
    }

    private void updateChildAncestors(DeptDO parentDept) {
        List<DeptDO> children = list(new LambdaQueryWrapper<DeptDO>().eq(DeptDO::getParentId, parentDept.getId()));
        for (DeptDO child : children) {
            child.setAncestors(parentDept.getAncestors() + "," + parentDept.getId());
            updateById(child);
            updateChildAncestors(child);
        }
    }

    private void fillUserCount(List<DeptDO> depts) {
        if (depts.isEmpty()) {
            return;
        }
        List<UserDO> users = userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .select(UserDO::getId, UserDO::getDeptId)
                .isNotNull(UserDO::getDeptId));
        Map<Long, Long> countMap = users.stream()
                .collect(Collectors.groupingBy(UserDO::getDeptId, Collectors.counting()));
        depts.forEach(d -> d.setUserCount(countMap.getOrDefault(d.getId(), 0L)));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        DeptDO dept = requireVisibleDept(id);
        dept.setStatus(status);
        updateById(dept);
    }

    @Override
    public PageResult<DeptDO> recyclePage(PageParam pageParam, String name, Integer status) {
        Page<DeptDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        DataScopeService.DataScopeContext scope = dataScopeService.getCurrentScope();
        List<Long> visibleDeptIds = scope.all() ? null : dataScopeService.getVisibleDeptIds();
        Page<DeptDO> deletedPage = (Page<DeptDO>) baseMapper.selectDeletedPage(
                page, name, status, visibleDeptIds);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    public void restore(Long id) {
        DeptDO deleted = baseMapper.selectDeletedById(id);
        if (deleted == null) {
            throw new BusinessException(404, "回收站部门不存在");
        }
        if (!dataScopeService.isDeptVisible(id)) {
            throw new BusinessException(403, "无权恢复该部门");
        }
        if (baseMapper.restoreById(id) == 0) {
            throw new BusinessException(404, "回收站部门不存在");
        }
    }

    @Override
    public void deletePermanent(Long id) {
        DeptDO deleted = baseMapper.selectDeletedById(id);
        if (deleted == null) {
            throw new BusinessException(404, "回收站部门不存在");
        }
        if (!dataScopeService.isDeptVisible(id)) {
            throw new BusinessException(403, "无权删除该部门");
        }
        if (baseMapper.deletePhysicalById(id) == 0) {
            throw new BusinessException(404, "回收站部门不存在");
        }
    }

    @Override
    public void syncLeaderDisplayName(String previousName, String newName) {
        if (!StringUtils.hasText(previousName) || !StringUtils.hasText(newName)) {
            return;
        }
        String oldName = previousName.trim();
        String nextName = newName.trim();
        if (oldName.equals(nextName)) {
            return;
        }
        update(new LambdaUpdateWrapper<DeptDO>()
                .set(DeptDO::getLeaderName, nextName)
                .eq(DeptDO::getLeaderName, oldName));
    }

    @Override
    public void syncLeaderByUserId(Long userId, String previousName, String newNickname) {
        if (userId == null || !StringUtils.hasText(newNickname)) {
            return;
        }
        String nextName = newNickname.trim();
        update(new LambdaUpdateWrapper<DeptDO>()
                .set(DeptDO::getLeaderName, nextName)
                .eq(DeptDO::getLeaderUserId, userId));
        syncLeaderDisplayName(previousName, nextName);
    }

    private void fillLeaderNames(List<DeptDO> depts) {
        if (depts == null || depts.isEmpty()) {
            return;
        }
        Set<Long> userIds = depts.stream()
                .map(DeptDO::getLeaderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<UserDO> users = userMapper.selectByIds(userIds);
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<Long, UserDO> userMap = users.stream().collect(Collectors.toMap(UserDO::getId, u -> u));
        for (DeptDO dept : depts) {
            if (dept.getLeaderUserId() == null) {
                continue;
            }
            UserDO user = userMap.get(dept.getLeaderUserId());
            if (user != null) {
                dept.setLeaderName(resolveUserDisplayName(user));
            }
        }
    }

    private void resolveLeaderBinding(DeptDO dept) {
        if (dept.getLeaderUserId() != null) {
            UserDO user = userMapper.selectById(dept.getLeaderUserId());
            if (user != null) {
                dept.setLeaderName(resolveUserDisplayName(user));
            }
            return;
        }
        if (!StringUtils.hasText(dept.getLeaderName())) {
            dept.setLeaderUserId(null);
            dept.setLeaderName(null);
            return;
        }
        String leaderName = dept.getLeaderName().trim();
        UserDO matched = findLeaderUser(leaderName, dept.getId());
        if (matched != null) {
            dept.setLeaderUserId(matched.getId());
            dept.setLeaderName(resolveUserDisplayName(matched));
        }
    }

    private UserDO findLeaderUser(String leaderName, Long deptId) {
        if (deptId != null) {
            UserDO inDept = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                    .eq(UserDO::getDeptId, deptId)
                    .and(w -> w.eq(UserDO::getNickname, leaderName).or().eq(UserDO::getUsername, leaderName))
                    .last("LIMIT 1"));
            if (inDept != null) {
                return inDept;
            }
        }
        return userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .and(w -> w.eq(UserDO::getNickname, leaderName).or().eq(UserDO::getUsername, leaderName))
                .last("LIMIT 1"));
    }

    private String resolveUserDisplayName(UserDO user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    @Override
    public List<Long> listSelfAndDescendantIds(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        List<DeptDO> all = list(new LambdaQueryWrapper<DeptDO>().select(DeptDO::getId, DeptDO::getParentId));
        Map<Long, List<DeptDO>> parentMap = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        Set<Long> ids = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || !ids.add(current)) {
                continue;
            }
            for (DeptDO child : parentMap.getOrDefault(current, List.of())) {
                if (child.getId() != null) {
                    queue.add(child.getId());
                }
            }
        }
        return new ArrayList<>(ids);
    }

    /**
     * 组装部门树。
     * <p>按数据范围过滤后，父部门可能不在结果集中（例如只能看到自己的部门），
     * 此时把这类节点提升为根节点，避免整棵树因为找不到 parentId=0 的根而变成空列表。</p>
     */
    private List<DeptDO> buildTree(List<DeptDO> depts) {
        if (depts == null || depts.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> ids = depts.stream()
                .map(DeptDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, List<DeptDO>> parentMap = depts.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        depts.forEach(d -> d.setChildren(parentMap.getOrDefault(d.getId(), new ArrayList<>())));
        return depts.stream()
                .filter(d -> !ids.contains(d.getParentId() == null ? 0L : d.getParentId()))
                .collect(Collectors.toList());
    }
}
