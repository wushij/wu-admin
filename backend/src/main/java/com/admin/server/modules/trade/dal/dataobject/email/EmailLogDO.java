package com.admin.server.modules.trade.dal.dataobject.email;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_email_log")
public class EmailLogDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;

    private String subject;

    private String content;

    private String scene;

    private String provider;

    /** 1-成功 2-失败 */
    private Integer status;

    private String resultMsg;

    private String ip;

    private LocalDateTime createTime;
}
