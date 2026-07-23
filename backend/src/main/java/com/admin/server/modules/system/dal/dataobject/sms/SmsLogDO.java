package com.admin.server.modules.system.dal.dataobject.sms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_sms_log")
public class SmsLogDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String content;

    private String smsType;

    private String templateId;

    private String templateParams;

    private String provider;

    /** 0-发送中 1-成功 2-失败 */
    private Integer status;

    private String resultMsg;

    private String bizId;

    private LocalDateTime sendTime;

    private Long userId;

    private String bizType;

    private String ip;

    private LocalDateTime createTime;
}
