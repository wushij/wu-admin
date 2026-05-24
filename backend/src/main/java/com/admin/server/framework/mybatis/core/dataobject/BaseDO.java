package com.admin.server.framework.mybatis.core.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础 DO 实体基类
 * <p>
 * 包含主键与 MyBatis-Plus 自动填充的审计字段（创建时间、更新时间、创建人、更新人、逻辑删除）。
 * </p>
 */
@Data
public abstract class BaseDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String creator;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    @TableLogic
    private Integer deleted;
}
