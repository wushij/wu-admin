package cn.rbac.server.modules.system.service.export.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class LoginLogExportRow {
    @ExcelProperty("ID")
    @ColumnWidth(10)
    private Long id;
    @ExcelProperty("用户名")
    @ColumnWidth(14)
    private String username;
    @ExcelProperty("IP")
    @ColumnWidth(16)
    private String ipaddr;
    @ExcelProperty("归属地")
    @ColumnWidth(18)
    private String loginLocation;
    @ExcelProperty("浏览器")
    @ColumnWidth(24)
    private String browser;
    @ExcelProperty("操作系统")
    @ColumnWidth(18)
    private String os;
    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;
    @ExcelProperty("提示消息")
    @ColumnWidth(24)
    private String msg;
    @ExcelProperty("登录时间")
    @ColumnWidth(20)
    private String loginTime;
}
