package cn.rbac.server.framework.storage;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 按扩展名解析 MIME，避免 Windows 上 probeContentType 为空导致浏览器把 zip 等当文本打开。
 */
public final class FileContentTypes {

    private static final Map<String, String> BY_EXT = Map.ofEntries(
            Map.entry(".zip", "application/zip"),
            Map.entry(".rar", "application/vnd.rar"),
            Map.entry(".7z", "application/x-7z-compressed"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry(".txt", MediaType.TEXT_PLAIN_VALUE),
            Map.entry(".json", MediaType.APPLICATION_JSON_VALUE),
            Map.entry(".xml", MediaType.APPLICATION_XML_VALUE),
            Map.entry(".png", MediaType.IMAGE_PNG_VALUE),
            Map.entry(".jpg", MediaType.IMAGE_JPEG_VALUE),
            Map.entry(".jpeg", MediaType.IMAGE_JPEG_VALUE),
            Map.entry(".gif", MediaType.IMAGE_GIF_VALUE),
            Map.entry(".webp", "image/webp"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".mp3", "audio/mpeg")
    );

    private FileContentTypes() {
    }

    public static String byFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String ext = LocalFileStorage.getSuffix(fileName).toLowerCase();
        return BY_EXT.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    public static String resolve(String fileName, String uploadedType, String probedType) {
        if (StringUtils.hasText(uploadedType) && !isGenericMime(uploadedType)) {
            return uploadedType;
        }
        if (StringUtils.hasText(probedType) && !isGenericMime(probedType)) {
            return probedType;
        }
        return byFileName(fileName);
    }

    public static boolean isGenericMime(String mime) {
        if (!StringUtils.hasText(mime)) {
            return true;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(mime)
                || MediaType.TEXT_PLAIN_VALUE.equals(mime)
                || "application/download".equals(mime);
    }

    public static boolean isInlineMime(String mime) {
        return StringUtils.hasText(mime)
                && (mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/"));
    }

    public static boolean shouldForceDownload(String mime) {
        return !isInlineMime(mime);
    }
}
