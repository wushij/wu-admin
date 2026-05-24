package cn.rbac.server.modules.system.dal.dataobject.permission;

import cn.rbac.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class MenuDO extends BaseEntity {
    private String name;
    private String permission;
    private Integer type;
    private Integer sort;
    private Long parentId;
    private String path;
    private String icon;
    private Integer status;
    private String component;
}
