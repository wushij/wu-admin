package cn.rbac.server.modules.system.service.export.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class UserExportRow {
    @ExcelProperty("用户ID")
    @ColumnWidth(10)
    private Long id;
    @ExcelProperty("用户名")
    @ColumnWidth(16)
    private String username;
    @ExcelProperty("昵称")
    @ColumnWidth(14)
    private String nickname;
    @ExcelProperty("手机号")
    @ColumnWidth(14)
    private String mobile;
    @ExcelProperty("邮箱")
    @ColumnWidth(22)
    private String email;
    @ExcelProperty("部门")
    @ColumnWidth(18)
    private String deptName;
    @ExcelProperty("岗位")
    @ColumnWidth(20)
    private String postNames;
    @ExcelProperty("角色")
    @ColumnWidth(20)
    private String roleNames;
    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;
    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    private String createTime;
}
