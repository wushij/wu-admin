package cn.rbac.server.modules.system.api.auth.vo;

import lombok.Data;

@Data
public class ProfileMobileBindReqVO {
    private String mobile;
    private String smsCode;
}
