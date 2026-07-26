package com.admin.server.modules.system.convert;

import com.admin.server.modules.system.api.dept.vo.DeptRespVO;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 部门数据转换器
 */
public final class DeptConvert {

    private DeptConvert() {
    }

    /**
     * 部门数据递归复制，防止 children 集合泛型在 BeanUtils 复制时丢失类型
     */
    public static DeptRespVO convertDept(DeptDO dept) {
        if (dept == null) {
            return null;
        }
        DeptRespVO vo = new DeptRespVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setName(dept.getName());
        vo.setAncestors(dept.getAncestors());
        vo.setSort(dept.getSort());
        vo.setLeaderName(dept.getLeaderName());
        vo.setLeaderUserId(dept.getLeaderUserId());
        vo.setPhone(dept.getPhone());
        vo.setEmail(dept.getEmail());
        vo.setStatus(dept.getStatus());
        vo.setUserCount(dept.getUserCount());
        vo.setCreateTime(dept.getCreateTime());
        vo.setUpdateTime(dept.getUpdateTime());
        if (dept.getChildren() != null) {
            List<DeptRespVO> childVOs = new ArrayList<>(dept.getChildren().size());
            for (DeptDO child : dept.getChildren()) {
                childVOs.add(convertDept(child));
            }
            vo.setChildren(childVOs);
        }
        return vo;
    }

    public static List<DeptRespVO> convertDeptList(List<DeptDO> deptList) {
        if (deptList == null || deptList.isEmpty()) {
            return Collections.emptyList();
        }
        List<DeptRespVO> list = new ArrayList<>(deptList.size());
        for (DeptDO dept : deptList) {
            list.add(convertDept(dept));
        }
        return list;
    }
}
