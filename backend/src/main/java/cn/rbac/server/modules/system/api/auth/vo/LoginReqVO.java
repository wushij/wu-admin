package cn.rbac.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginReqVO {
    /** account | sms */
    private String loginType;
    private String username;
    private String password;
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    private String uuid;
    private String code;
}
