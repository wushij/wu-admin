package com.admin.server.modules.system.api.menu.vo;

import lombok.Data;

@Data
public class MenuCreateReqVO {
    private String name;
    private String permission;
    private Integer type;
    private Integer sort;
    private Long parentId;
    private String path;
    private String icon;
    private String component;
    private Integer status;
}
