package com.admin.server.modules.infra.api.gen.vo;

import lombok.Data;

@Data
public class GenTableColumnRespVO {
    private Long id;
    private Long tableId;
    private String columnName;
    private String columnComment;
    private String columnType;
    private String javaType;
    private String javaField;
    private Integer isPk;
    private Integer isIncrement;
    private Integer isRequired;
    private Integer isInsert;
    private Integer isEdit;
    private Integer isList;
    private Integer isQuery;
    private String queryType;
    private String htmlType;
    private String dictType;
    private Integer sort;
}
