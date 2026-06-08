package cn.rbac.server.modules.system.service.role.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.role.vo.RoleCreateReqVO;
import cn.rbac.server.modules.system.api.role.vo.RoleUpdateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.rbac.server.modules.system.service.role.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PermissionService permissionService;

    @Override
    public List<RoleDO> list(String name, Integer status) {
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(RoleDO::getName, name);
        }
        if (status != null) {
            wrapper.eq(RoleDO::getStatus, status);
        }
        List<RoleDO> roles = roleMapper.selectList(wrapper);
        roles.forEach(role -> role.setMenuIds(permissionService.getRoleMenuListByRoleId(role.getId())));
        return roles;
    }

    @Override
    public PageResult<RoleDO> page(PageParam pageParam) {
        Page<RoleDO> page = roleMapper.selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), null);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    @Override
    public RoleDO getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public Long create(RoleCreateReqVO reqVO) {
        RoleDO role = new RoleDO();
        role.setName(reqVO.getName());
        role.setCode(reqVO.getCode());
        role.setSort(reqVO.getSort());
        role.setStatus(1);
        role.setRemark(reqVO.getRemark());
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    public void update(RoleUpdateReqVO reqVO) {
        RoleDO role = roleMapper.selectById(reqVO.getId());
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        role.setName(reqVO.getName());
        role.setCode(reqVO.getCode());
        role.setSort(reqVO.getSort());
        role.setStatus(reqVO.getStatus());
        role.setRemark(reqVO.getRemark());
        roleMapper.updateById(role);
    }

    @Override
    public void delete(Long id) {
        roleMapper.deleteById(id);
    }

    @Override
    public PageResult<RoleDO> recyclePage(PageParam pageParam, String name, Integer status) {
        Page<RoleDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<RoleDO> deletedPage = (Page<RoleDO>) roleMapper.selectDeletedPage(page, name, status);
        List<RoleDO> roles = deletedPage.getRecords();
        roles.forEach(role -> role.setMenuIds(permissionService.getRoleMenuListByRoleId(role.getId())));
        return PageResult.of(roles, deletedPage.getTotal());
    }

    @Override
    public void restore(Long id) {
        int rows = roleMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站角色不存在");
        }
    }

    @Override
    public void deletePermanent(Long id) {
        int rows = roleMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站角色不存在");
        }
    }

    @Override
    public Set<Long> getMenuIdsForAssign(Long roleId) {
        return permissionService.getRoleMenuIdsForAssign(roleId);
    }

    @Override
    public void assignMenu(Long roleId, Set<Long> menuIds) {
        permissionService.assignRoleMenu(roleId, menuIds);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        role.setStatus(status);
        roleMapper.updateById(role);
    }
}
