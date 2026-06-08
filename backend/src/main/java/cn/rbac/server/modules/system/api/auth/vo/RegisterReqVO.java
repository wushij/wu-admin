package cn.rbac.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterReqVO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 30, message = "用户名长度 2~30 个字符")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6~50 个字符")
    private String password;
    private String nickname;
    private String uuid;
    private String code;
}
