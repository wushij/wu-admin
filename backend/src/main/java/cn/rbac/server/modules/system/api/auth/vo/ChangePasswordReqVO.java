package cn.rbac.server.modules.system.api.auth.vo;

import lombok.Data;

@Data
public class ChangePasswordReqVO {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
