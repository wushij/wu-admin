package cn.rbac.server.modules.system.dal.dataobject.gen;

import lombok.Data;

@Data
public class DatabaseColumnVO {
    private String columnName;
    private String columnComment;
    private String dataType;
    private String columnType;
    private String isNullable;
    private String columnKey;
    private String extra;
    private Integer ordinalPosition;
}
