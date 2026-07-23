package com.admin.server.modules.system.api.auth.vo;

import lombok.Data;

@Data
public class ProfileUpdateReqVO {
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
}
