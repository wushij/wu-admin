package cn.rbac.server.modules.system.api.auth.vo;

import lombok.Data;

@Data
public class ProfilePasswordSmsResetReqVO {
    private String smsCode;
    private String newPassword;
    private String confirmPassword;
}
