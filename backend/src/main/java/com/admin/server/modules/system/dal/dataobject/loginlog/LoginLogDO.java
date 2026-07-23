package com.admin.server.modules.system.dal.dataobject.loginlog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志表
 */
@Data
@TableName("sys_login_log")
public class LoginLogDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * IP地址
     */
    private String ipaddr;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 登录状态(0-成功 1-失败)
     */
    private Integer status;

    /**
     * 消息
     */
    private String msg;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
}
