package cn.rbac.server.modules.system.service.dashboard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentLoginVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String ipaddr;
    private String loginLocation;
    private String browser;
    private String os;
    private Integer status;
    private LocalDateTime loginTime;
}
