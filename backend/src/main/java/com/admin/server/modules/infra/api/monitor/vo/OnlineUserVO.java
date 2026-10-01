package com.admin.server.modules.infra.api.monitor.vo;

import lombok.Data;

@Data
public class OnlineUserVO {
    /** 用户编号（展示用） */
    private Long userId;
    private String loginName;
    private String nickname;
    private String avatar;
    private String deptName;
    private String ipaddr;
    private String loginLocation;
    private String browser;
    private String os;
    private Integer status;
    private String loginTime;
    private String lastAccessTime;
    /** 强退时传用户 ID */
    private String tokenId;
}
