package cn.rbac.server.modules.system.service.dept.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.dept.DeptMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.dept.DeptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            updateById(dept);
            updateChildAncestors(dept);
        } else {
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

    private List<DeptDO> buildTree(List<DeptDO> depts) {
        Map<Long, List<DeptDO>> parentMap = depts.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        depts.forEach(d -> d.setChildren(parentMap.get(d.getId())));
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }
}
