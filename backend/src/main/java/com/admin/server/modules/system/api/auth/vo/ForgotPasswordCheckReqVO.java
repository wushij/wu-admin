package com.admin.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordCheckReqVO {
    @NotBlank(message = "用户名不能为空")
    private String username;
}
