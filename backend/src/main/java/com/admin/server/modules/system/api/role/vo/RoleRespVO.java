package com.admin.server.modules.system.api.role.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RoleRespVO {
    private Long id;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String remark;
    private Integer dataScope;
    private String dataScopeDeptIds;
    private Set<Long> menuIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
