package cn.rbac.server.modules.system.dal.dataobject.dept;

import cn.rbac.server.common.mybatis.BaseEntity;
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
    private Integer sort;
    private Integer status;
    private String leaderName;
    private String phone;
    private String email;
    
    /**
     * 子部门列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeptDO> children;
}
