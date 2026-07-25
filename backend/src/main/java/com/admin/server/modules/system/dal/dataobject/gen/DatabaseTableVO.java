package com.admin.server.modules.system.dal.dataobject.gen;

import lombok.Data;

@Data
public class DatabaseTableVO {
    private String tableName;
    private String tableComment;
    private String createTime;
    private String updateTime;
}
