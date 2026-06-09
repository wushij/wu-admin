package cn.rbac.server.modules.system.api.auth.vo;

import lombok.Data;

@Data
public class ProfilePasswordSmsCodeReqVO {
    /** 滑块验证通过时传 slider_verified */
    private String code;
}
