package com.admin.server.modules.system.dal.dataobject.post;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class PostDO extends BaseEntity {
    private Long parentId;
    private String postCode;
    private String postName;
    private Integer sort;
    private Integer status;
    private String remark;

    @TableField(exist = false)
    private List<PostDO> children;

    @TableField(exist = false)
    private Long userCount;
}
