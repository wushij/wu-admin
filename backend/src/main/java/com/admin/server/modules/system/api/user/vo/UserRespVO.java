package com.admin.server.modules.system.api.user.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class UserRespVO {
    private Long id;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
    private Integer status;
    private Long deptId;
    private String deptName;
    private Set<Long> roleIds;
    private String postNames;
    private List<Long> postIds;
    private Boolean loginLocked;
    private Long loginLockRemainSeconds;
    private Integer loginFailCount;
    private String loginRecentIp;
    private Boolean loginIpLocked;
    private Long loginIpLockRemainSeconds;
    private Integer loginIpFailCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
