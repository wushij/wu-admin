package cn.rbac.server.modules.system.service.export.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class TicketExportRow {
    @ExcelProperty("工单号")
    @ColumnWidth(18)
    private String ticketNo;
    @ExcelProperty("标题")
    @ColumnWidth(24)
    private String title;
    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String status;
    @ExcelProperty("优先级")
    @ColumnWidth(12)
    private String priority;
    @ExcelProperty("创建人")
    @ColumnWidth(14)
    private String creatorName;
    @ExcelProperty("处理人")
    @ColumnWidth(14)
    private String assigneeName;
    @ExcelProperty("截止时间")
    @ColumnWidth(20)
    private String deadline;
    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    private String createTime;
}
