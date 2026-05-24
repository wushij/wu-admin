package com.admin.server.modules.system.api.dict.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DictTypeRespVO {
    private Long id;
    private String dictName;
    private String dictType;
    private Integer status;
    private String remark;
    private Long dataCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
