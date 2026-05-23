package cn.rbac.server.modules.system.dal.dataobject.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_file_group")
public class SysFileGroupDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer sort;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer fileCount;
}
