package cn.rbac.server.modules.system.service.export.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class ApiAccessExportRow {
    @ExcelProperty("ID")
    @ColumnWidth(10)
    private Long id;
    @ExcelProperty("用户")
    @ColumnWidth(14)
    private String username;
    @ExcelProperty("方法")
    @ColumnWidth(8)
    private String method;
    @ExcelProperty("接口路径")
    @ColumnWidth(32)
    private String apiPath;
    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String successText;
    @ExcelProperty("耗时(ms)")
    @ColumnWidth(12)
    private Long durationMs;
    @ExcelProperty("IP")
    @ColumnWidth(16)
    private String clientIp;
    @ExcelProperty("访问时间")
    @ColumnWidth(20)
    private String startTime;
}
