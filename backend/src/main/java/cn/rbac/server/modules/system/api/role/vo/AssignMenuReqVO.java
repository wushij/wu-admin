package cn.rbac.server.modules.system.api.role.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class AssignMenuReqVO {
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    @NotNull(message = "菜单ID不能为空")
    private Set<Long> menuIds;
}
