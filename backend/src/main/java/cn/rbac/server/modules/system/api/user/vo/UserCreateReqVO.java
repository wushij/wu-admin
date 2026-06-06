package cn.rbac.server.modules.system.api.user.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateReqVO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 30, message = "用户名长度 2~30 个字符")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6~50 个字符")
    private String password;
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称最多 30 个字符")
    private String nickname;
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
    private String email;
    private Long deptId;
    private Long roleId;
    private List<Long> postIds;
}
