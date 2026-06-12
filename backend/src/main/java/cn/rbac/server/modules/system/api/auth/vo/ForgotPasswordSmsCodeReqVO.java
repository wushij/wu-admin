package cn.rbac.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordSmsCodeReqVO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    /** 滑块验证通过时传 slider_verified */
    private String code;
}
