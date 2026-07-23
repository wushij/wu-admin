package com.admin.server.modules.system.service.dept.impl;

import com.admin.server.common.pojo.BusinessException;
import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.dept.DeptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    public List<DeptDO> tree(String name, Integer status) {
        LambdaQueryWrapper<DeptDO> wrapper = new LambdaQueryWrapper<>();
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
        return list(new LambdaQueryWrapper<DeptDO>().orderByAsc(DeptDO::getSort));
    }

    @Override
    public DeptDO getById(Long id) {
        DeptDO dept = super.getById(id);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        fillLeaderNames(List.of(dept));
        return dept;
    }

    @Override
    public void create(DeptDO dept) {
        Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
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
        DeptDO exist = super.getById(dept.getId());
        if (exist == null) {
            throw new BusinessException(404, "部门不存在");
        }
        Long parentId = dept.getParentId() == null ? exist.getParentId() : dept.getParentId();
        if (parentId == null) {
            parentId = 0L;
        }
        if (dept.getId().equals(parentId)) {
            throw new BusinessException("上级部门不能选择自己");
        }
        if (!parentId.equals(exist.getParentId() == null ? 0L : exist.getParentId())) {
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
        long childCount = count(new LambdaQueryWrapper<DeptDO>().eq(DeptDO::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("存在子部门，无法删除");
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
        DeptDO dept = super.getById(id);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        parentId = parentId == null ? 0L : parentId;
        if (id.equals(parentId)) {
            throw new BusinessException("上级部门不能选择自己");
        }
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
        DeptDO dept = getById(id);
        dept.setStatus(status);
        updateById(dept);
    }

    @Override
    public PageResult<DeptDO> recyclePage(PageParam pageParam, String name, Integer status) {
        Page<DeptDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<DeptDO> deletedPage = (Page<DeptDO>) baseMapper.selectDeletedPage(page, name, status);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    public void restore(Long id) {
        if (baseMapper.restoreById(id) == 0) {
            throw new BusinessException(404, "回收站部门不存在");
        }
    }

    @Override
    public void deletePermanent(Long id) {
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

    private List<DeptDO> buildTree(List<DeptDO> depts) {
        Map<Long, List<DeptDO>> parentMap = depts.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        depts.forEach(d -> d.setChildren(parentMap.get(d.getId())));
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }
}
