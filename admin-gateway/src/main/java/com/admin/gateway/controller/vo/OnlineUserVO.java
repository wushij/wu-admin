package com.admin.gateway.controller.vo;

import lombok.Data;

@Data
public class OnlineUserVO {
    private String tokenId;
    private String loginName;
    private String deptName;
    private String ipaddr;
    private String loginLocation;
    private String browser;
    private String os;
    private Integer status;
    private String loginTime;
    private String lastAccessTime;
    private String tokenValue;
}
