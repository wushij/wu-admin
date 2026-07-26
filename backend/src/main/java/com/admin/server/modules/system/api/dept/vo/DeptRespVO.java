package com.admin.server.modules.system.api.dept.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeptRespVO {
    private Long id;
    private Long parentId;
    private String name;
    private String ancestors;
    private Integer sort;
    private String leaderName;
    private Long leaderUserId;
    private String phone;
    private String email;
    private Integer status;
    private List<DeptRespVO> children;
    private Long userCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
