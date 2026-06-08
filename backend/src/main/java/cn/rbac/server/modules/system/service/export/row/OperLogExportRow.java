package cn.rbac.server.modules.system.service.export.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class OperLogExportRow {
    @ExcelProperty("ID")
    @ColumnWidth(10)
    private Long id;
    @ExcelProperty("模块")
    @ColumnWidth(16)
    private String title;
    @ExcelProperty("业务类型")
    @ColumnWidth(12)
    private String businessTypeText;
    @ExcelProperty("操作人")
    @ColumnWidth(14)
    private String operName;
    @ExcelProperty("请求方式")
    @ColumnWidth(10)
    private String requestMethod;
    @ExcelProperty("请求地址")
    @ColumnWidth(28)
    private String operUrl;
    @ExcelProperty("IP")
    @ColumnWidth(16)
    private String operIp;
    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;
    @ExcelProperty("耗时(ms)")
    @ColumnWidth(12)
    private Long costTime;
    @ExcelProperty("操作时间")
    @ColumnWidth(20)
    private String operTime;
}
