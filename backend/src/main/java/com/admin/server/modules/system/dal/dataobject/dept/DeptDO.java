package com.admin.server.modules.system.dal.dataobject.dept;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class DeptDO extends BaseEntity {
    private String name;
    private Long parentId;
    /** 祖级列表，如 0,1,5 */
    private String ancestors;
    private Integer sort;
    private Integer status;
    private String leaderName;
    private Long leaderUserId;
    private String phone;
    private String email;
    
    /**
     * 子部门列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeptDO> children;

    @TableField(exist = false)
    private Long userCount;
}
