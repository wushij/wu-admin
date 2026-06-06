package cn.rbac.server.modules.system.api.role.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateReqVO {
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 30, message = "角色名称最多 30 个字符")
    private String name;
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码最多 50 个字符")
    private String code;
    private Integer sort;
    private String remark;
}
