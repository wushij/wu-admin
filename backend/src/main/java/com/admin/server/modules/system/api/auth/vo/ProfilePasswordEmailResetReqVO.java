package com.admin.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfilePasswordEmailResetReqVO {

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    private String confirmPassword;
}
