package cn.rbac.server.modules.system.dal.dataobject.monitor;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API 访问统计日志
 */
@Data
@TableName("sys_api_access_log")
public class ApiAccessLogDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String apiPath;

    private String method;

    private Integer statusCode;

    /** 是否成功(0否 1是) */
    private Integer success;

    private Long costTime;

    private String ip;

    private Long userId;

    /** 登录名（非库字段，列表展示用） */
    @TableField(exist = false)
    private String username;
}
