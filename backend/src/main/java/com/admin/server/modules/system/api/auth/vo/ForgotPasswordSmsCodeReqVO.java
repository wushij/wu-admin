package com.admin.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordSmsCodeReqVO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    /** 滑块 challenge token */
    private String uuid;
    /** 滑块拖动 offsetX（整数） */
    private String code;
}
