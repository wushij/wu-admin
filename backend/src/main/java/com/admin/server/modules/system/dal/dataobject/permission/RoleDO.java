package com.admin.server.modules.system.dal.dataobject.permission;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class RoleDO extends BaseEntity {
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String remark;
    private Integer dataScope;
    private String dataScopeDeptIds;
    
    /**
     * 菜单ID列表（非数据库字段，用于展示）
     */
    @TableField(exist = false)
    private Set<Long> menuIds;
}
