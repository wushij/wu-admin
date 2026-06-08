package cn.rbac.server.framework.export;

import com.alibaba.excel.support.ExcelTypeEnum;

public enum ExportFormat {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ExcelTypeEnum.XLSX),
    CSV("csv", "text/csv;charset=UTF-8", ExcelTypeEnum.CSV);

    private final String extension;
    private final String contentType;
    private final ExcelTypeEnum excelType;

    ExportFormat(String extension, String contentType, ExcelTypeEnum excelType) {
        this.extension = extension;
        this.contentType = contentType;
        this.excelType = excelType;
    }

    public static ExportFormat fromParam(String format) {
        if (format == null || format.isBlank()) {
            return XLSX;
        }
        return "csv".equalsIgnoreCase(format.trim()) ? CSV : XLSX;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public ExcelTypeEnum getExcelType() {
        return excelType;
    }
}
