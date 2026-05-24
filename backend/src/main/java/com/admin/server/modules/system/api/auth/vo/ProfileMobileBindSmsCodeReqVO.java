package com.admin.server.modules.system.api.auth.vo;

import lombok.Data;

@Data
public class ProfileMobileBindSmsCodeReqVO {
    private String mobile;
    /** 滑块 challenge token */
    private String uuid;
    /** 滑块拖动 offsetX（整数） */
    private String code;
}
