package com.admin.server.modules.system.dal.dataobject.file;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFileDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String originalName;
    private String fileName;
    private String filePath;
    private String url;
    private Long fileSize;
    private String fileType;
    private String fileSuffix;
    private String storageType;
    private String bucketName;
    private Long groupId;
    private String remark;
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
