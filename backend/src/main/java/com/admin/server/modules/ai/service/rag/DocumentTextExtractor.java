package com.admin.server.modules.ai.service.rag;

import cn.hutool.core.util.StrUtil;
import com.admin.server.common.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * 知识库文档正文解析器。
 * <p>支持 Markdown / TXT / PDF / Word(.docx/.doc)。文件原文由文件中心保存，
 * 本类只负责把字节流解析为纯文本，解析结果写入文档表的 content 字段。</p>
 */
@Component
public class DocumentTextExtractor {

    /** 允许上传并解析的扩展名 */
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "md", "markdown", "txt", "pdf", "docx", "doc");

    /** 单篇文档正文字符上限，超出截断，避免单文档挤占存储与向量成本 */
    public static final int MAX_CONTENT_CHARS = 200_000;

    /**
     * 解析正文。
     *
     * @param bytes    文件字节
     * @param fileName 原始文件名（用于判定扩展名）
     */
    public String extract(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(400, "文件内容为空");
        }
        String ext = resolveExtension(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "暂不支持该文件格式：" + ext
                    + "，请上传 PDF、Word(.docx)、Markdown 或 TXT 文档");
        }
        String text;
        try {
            text = switch (ext) {
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                case "doc" -> extractDoc(bytes);
                default -> decodeText(bytes);
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "解析文档失败（" + ext + "）：" + e.getMessage());
        }
        String normalized = normalizeText(text);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException(400, "文档未解析出可用正文，请确认文件不是扫描件或加密文档");
        }
        if (normalized.length() > MAX_CONTENT_CHARS) {
            normalized = normalized.substring(0, MAX_CONTENT_CHARS);
        }
        return normalized;
    }

    /** 解析文件扩展名，无法识别时返回空串 */
    public static String resolveExtension(String fileName) {
        String name = StrUtil.nullToEmpty(fileName).trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String extractPdf(byte[] bytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(sb, paragraph.getText());
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    StringBuilder rowText = new StringBuilder();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        if (rowText.length() > 0) {
                            rowText.append(" | ");
                        }
                        rowText.append(StrUtil.nullToEmpty(cell.getText()).replace('\n', ' ').trim());
                    }
                    appendLine(sb, rowText.toString());
                }
            }
        }
        return sb.toString();
    }

    private String extractDoc(byte[] bytes) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    /** 文本类文件按 UTF-8 严格解码，失败后依次尝试 GB18030 与宽松 UTF-8 */
    private String decodeText(byte[] bytes) {
        for (String charsetName : new String[]{"UTF-8", "GB18030"}) {
            try {
                Charset charset = Charset.forName(charsetName);
                CharBuffer decoded = charset.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes));
                return decoded.toString();
            } catch (CharacterCodingException ignored) {
                // 继续尝试下一个字符集
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void appendLine(StringBuilder sb, String line) {
        String trimmed = StrUtil.nullToEmpty(line).trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(trimmed);
    }

    /** 统一换行、压缩连续空行、去掉首尾空白 */
    private String normalizeText(String text) {
        String normalized = StrUtil.nullToEmpty(text)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        normalized = normalized.replaceAll("[\\t\\x0B\\f]+", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        if (!normalized.isEmpty() && normalized.charAt(0) == '\uFEFF') {
            normalized = normalized.substring(1);
        }
        return normalized.trim();
    }
}
