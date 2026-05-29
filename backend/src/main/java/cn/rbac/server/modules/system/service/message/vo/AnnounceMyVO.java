package cn.rbac.server.modules.system.service.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnounceMyVO {
    private Long id;
    private String title;
    private String content;
    private Integer noticeType;
    private Integer status;
    private String createName;
    private LocalDateTime createTime;
    private Integer isRead;
    private LocalDateTime readTime;
}
