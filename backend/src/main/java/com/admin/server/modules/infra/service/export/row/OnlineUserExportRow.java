package com.admin.server.modules.infra.service.export.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class OnlineUserExportRow {
    @ExcelProperty("用户ID")
    @ColumnWidth(12)
    private Long userId;
    @ExcelProperty("用户名")
    @ColumnWidth(14)
    private String loginName;
    @ExcelProperty("部门/昵称")
    @ColumnWidth(18)
    private String deptName;
    @ExcelProperty("主机")
    @ColumnWidth(16)
    private String ipaddr;
    @ExcelProperty("登录地点")
    @ColumnWidth(18)
    private String loginLocation;
    @ExcelProperty("浏览器")
    @ColumnWidth(24)
    private String browser;
    @ExcelProperty("操作系统")
    @ColumnWidth(18)
    private String os;
    @ExcelProperty("会话状态")
    @ColumnWidth(12)
    private String statusText;
    @ExcelProperty("登录时间")
    @ColumnWidth(20)
    private String loginTime;
    @ExcelProperty("最后访问时间")
    @ColumnWidth(20)
    private String lastAccessTime;
}
