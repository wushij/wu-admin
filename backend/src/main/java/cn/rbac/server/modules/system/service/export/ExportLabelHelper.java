package cn.rbac.server.modules.system.service.export;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class ExportLabelHelper {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<Integer, String> OPER_BUSINESS_TYPE = Map.of(
            0, "其他", 1, "新增", 2, "修改", 3, "删除", 4, "查询", 5, "导出", 6, "导入");

    private ExportLabelHelper() {
    }

    public static String formatDateTime(LocalDateTime time) {
        return time == null ? "" : time.format(DT);
    }

    public static String userStatus(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "停用";
            case 1 -> "正常";
            case 2 -> "待审核";
            case 3 -> "审核驳回";
            default -> String.valueOf(status);
        };
    }

    public static String successFlag(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 0 ? "成功" : "失败";
    }

    public static String apiSuccess(Integer success) {
        if (success == null) {
            return "";
        }
        return success == 1 ? "成功" : "失败";
    }

    public static String operBusinessType(Integer type) {
        if (type == null) {
            return "其他";
        }
        return OPER_BUSINESS_TYPE.getOrDefault(type, "其他");
    }

    public static String onlineStatus(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 1 ? "在线" : "离线";
    }
}
