package com.admin.server.modules.system.api.user.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class AssignRoleReqVO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "角色ID不能为空")
    private Set<Long> roleIds;
}
