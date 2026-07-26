package com.admin.server.modules.system.api.post.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostRespVO {
    private Long id;
    private Long parentId;
    private String postCode;
    private String postName;
    private Integer sort;
    private Integer status;
    private String remark;
    private List<PostRespVO> children;
    private Long userCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
