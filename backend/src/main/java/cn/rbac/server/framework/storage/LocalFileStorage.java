package cn.rbac.server.framework.storage;

import cn.rbac.server.framework.config.DynamicConfigProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class LocalFileStorage {

    @Resource
    private FileStorageProperties properties;

    @Resource
    private DynamicConfigProvider dynamicConfigProvider;

    public String getBasePath() {
        return properties.getLocalPath();
    }

    public void validateUpload(String originalName, long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("文件为空");
        }
        int maxMb = dynamicConfigProvider.getFileMaxSizeMb();
        long maxBytes = (long) maxMb * 1024 * 1024;
        if (size > maxBytes) {
            throw new IllegalArgumentException("文件大小不能超过 " + maxMb + "MB");
        }
        if (!StringUtils.hasText(originalName)) {
            return;
        }
        String ext = getSuffix(originalName).replace(".", "").toLowerCase();
        String allowed = dynamicConfigProvider.getFileAllowedExtensions();
        if (!StringUtils.hasText(allowed) || !StringUtils.hasText(ext)) {
            return;
        }
        boolean ok = false;
        for (String item : allowed.split(",")) {
            if (ext.equals(item.trim().toLowerCase())) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("不允许上传该类型文件: ." + ext);
        }
    }

    public String upload(InputStream inputStream, String storagePath, String fileName) throws IOException {
        Path dir = Paths.get(properties.getLocalPath(), storagePath);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return "/api/files/" + storagePath + "/" + fileName;
    }

    public Path resolvePath(String filePath) {
        return resolveSafe(filePath);
    }

    public byte[] readBytes(String filePath) throws IOException {
        Path full = resolveSafe(filePath);
        return Files.readAllBytes(full);
    }

    public void delete(String filePath) throws IOException {
        Path full = resolveSafe(filePath);
        Files.deleteIfExists(full);
    }

    private Path resolveSafe(String filePath) {
        Path base = Paths.get(properties.getLocalPath()).normalize().toAbsolutePath();
        Path full = base.resolve(filePath.replace("\\", "/")).normalize();
        if (!full.startsWith(base)) {
            throw new SecurityException("非法文件路径");
        }
        return full;
    }

    public static String getSuffix(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }
}
