package com.admin.server.modules.system.api.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "个人中心 - 绑定/更换邮箱 Request VO")
@Data
public class ProfileEmailBindReqVO {

    @Schema(description = "新邮箱地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @Schema(description = "邮箱验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证码不能为空")
    private String code;
}
