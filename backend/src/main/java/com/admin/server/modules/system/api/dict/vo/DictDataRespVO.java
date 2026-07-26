package com.admin.server.modules.system.api.dict.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DictDataRespVO {
    private Long id;
    private Integer sort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private String cssClass;
    private String listClass;
    private Integer isDefault;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
