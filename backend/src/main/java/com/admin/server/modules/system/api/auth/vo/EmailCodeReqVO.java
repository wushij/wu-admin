package com.admin.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailCodeReqVO {

    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 滑块验证 Token */
    private String uuid;

    /** 滑块验证 Offset */
    private String code;
}
