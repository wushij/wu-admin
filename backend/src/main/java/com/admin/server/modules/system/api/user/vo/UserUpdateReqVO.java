package com.admin.server.modules.system.api.user.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateReqVO {
    @NotNull(message = "用户ID不能为空")
    private Long id;
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称最多 30 个字符")
    private String nickname;
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
    private String email;
    private Integer status;
    private Long deptId;
    private Long roleId;
    private List<Long> postIds;
}
