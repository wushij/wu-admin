package com.admin.server.modules.system.dal.dataobject.job;



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

@TableName("sys_job_log")

public class SysJobLogDO implements Serializable {

    @TableId(type = IdType.AUTO)

    private Long id;



    private String jobName;

    private String jobGroup;

    private String invokeTarget;

    private String jobMessage;

    /** 0 成功 / 1 失败 */

    private Integer status;

    private String exceptionInfo;

    private LocalDateTime startTime;

    private LocalDateTime stopTime;

    /** 执行耗时（毫秒） */

    private Long durationMs;



    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;



    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;



    @TableLogic

    private Integer deleted;

}

