package com.admin.server.modules.system.api.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "个人中心 - 发送邮箱验证码 Request VO")
@Data
public class ProfileEmailCodeReqVO {

    @Schema(description = "新邮箱地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    private String email;
}
