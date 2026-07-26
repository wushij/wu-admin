package com.admin.server.modules.infra.service.export.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class ApprovalExportRow {
    @ExcelProperty("单号")
    @ColumnWidth(20)
    private String formNo;
    @ExcelProperty("标题")
    @ColumnWidth(24)
    private String title;
    @ExcelProperty("类型")
    @ColumnWidth(12)
    private String formType;
    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String status;
    @ExcelProperty("申请人")
    @ColumnWidth(14)
    private String applicantName;
    @ExcelProperty("审批人")
    @ColumnWidth(14)
    private String approverName;
    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    private String createTime;
    @ExcelProperty("更新时间")
    @ColumnWidth(20)
    private String updateTime;
}
