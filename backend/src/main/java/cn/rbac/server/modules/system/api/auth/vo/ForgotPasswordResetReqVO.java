package cn.rbac.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordResetReqVO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
