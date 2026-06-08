package cn.rbac.server.framework.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;

public final class ExportHelper {

    public static final int MAX_EXPORT_ROWS = 10_000;
    public static final int BATCH_SIZE = 500;

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ExportHelper() {
    }

    public static String buildFilename(String baseName, ExportFormat format) {
        String safe = baseName == null || baseName.isBlank() ? "export" : baseName.trim();
        return safe + "_" + LocalDateTime.now().format(FILE_TIME) + "." + format.getExtension();
    }

    public static void setDownloadHeaders(HttpServletResponse response, String filename, ExportFormat format)
            throws IOException {
        response.setContentType(format.getContentType());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }

    public static <T> void writeOnce(HttpServletResponse response, ExportFormat format, String filename,
                                    Class<T> headClass, String sheetName, List<T> rows) throws IOException {
        setDownloadHeaders(response, filename, format);
        EasyExcel.write(response.getOutputStream(), headClass)
                .excelType(format.getExcelType())
                .sheet(sheetName)
                .doWrite(rows);
    }

    /**
     * 分批拉取并流式写入，避免一次性加载超大数据
     */
    public static <T> void writeBatched(HttpServletResponse response, ExportFormat format, String filename,
                                        Class<T> headClass, String sheetName,
                                        BiFunction<Integer, Integer, List<T>> batchFetcher) throws IOException {
        setDownloadHeaders(response, filename, format);
        ExcelWriter writer = EasyExcel.write(response.getOutputStream(), headClass)
                .excelType(format.getExcelType())
                .build();
        WriteSheet sheet = EasyExcel.writerSheet(sheetName).build();
        int pageNo = 1;
        int written = 0;
        try {
            while (written < MAX_EXPORT_ROWS) {
                int pageSize = Math.min(BATCH_SIZE, MAX_EXPORT_ROWS - written);
                List<T> batch = batchFetcher.apply(pageNo, pageSize);
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                writer.write(batch, sheet);
                written += batch.size();
                if (batch.size() < pageSize) {
                    break;
                }
                pageNo++;
            }
        } finally {
            writer.finish();
        }
    }
}
