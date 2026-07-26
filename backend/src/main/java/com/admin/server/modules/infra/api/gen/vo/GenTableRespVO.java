package com.admin.server.modules.infra.api.gen.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GenTableRespVO {
    private Long id;
    private String tableName;
    private String tableComment;
    private String className;
    private String packageName;
    private String moduleName;
    private String businessName;
    private String functionName;
    private String author;
    private String genType;
    private String genPath;
    private String frontType;
    private String formLayout;
    private Long parentMenuId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<GenTableColumnRespVO> columns;
    private GenTableColumnRespVO pkColumn;
}
