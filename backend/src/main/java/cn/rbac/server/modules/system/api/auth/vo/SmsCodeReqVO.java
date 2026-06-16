package cn.rbac.server.modules.system.api.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsCodeReqVO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    /** 滑块 challenge token（发码前滑块开启时必填，对应 GET /auth/slider-challenge） */
    private String uuid;
    /** 滑块拖动 offsetX（整数）；或短信验证码场景下的验证码 */
    private String code;
}
