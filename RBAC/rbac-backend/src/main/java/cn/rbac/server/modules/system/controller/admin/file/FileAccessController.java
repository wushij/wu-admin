package cn.rbac.server.modules.system.controller.admin.file;

import cn.rbac.server.framework.storage.FileStorageProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件直链访问 GET /files/**
 */
@RestController
@RequestMapping("/files")
public class FileAccessController {

    @Resource
    private FileStorageProperties fileStorageProperties;

    @GetMapping("/**")
    public ResponseEntity<byte[]> getFile(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        int idx = uri.indexOf("/files/");
        String relative = idx >= 0 ? uri.substring(idx + "/files/".length()) : "";
        String basePath = fileStorageProperties.getLocalPath();
        Path base = Paths.get(basePath).normalize().toAbsolutePath();
        Path full = base.resolve(relative).normalize();
        if (!full.startsWith(base) || !Files.exists(full)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        byte[] bytes = Files.readAllBytes(full);
        String contentType = Files.probeContentType(full);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, contentType).body(bytes);
    }
}
