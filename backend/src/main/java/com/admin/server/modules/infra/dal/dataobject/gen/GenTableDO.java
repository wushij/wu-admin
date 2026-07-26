package com.admin.server.modules.infra.dal.dataobject.gen;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("gen_table")
public class GenTableDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tableName;
    private String tableComment;
    private String className;
    private String packageName;
    private String moduleName;
    private String businessName;
    private String functionName;
    private String author;
    private String genType;
    private String genPath;
    private String frontType;
    private String formLayout;
    private Long parentMenuId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    @TableField(exist = false)
    private List<GenTableColumnDO> columns;

    @TableField(exist = false)
    private GenTableColumnDO pkColumn;
}
