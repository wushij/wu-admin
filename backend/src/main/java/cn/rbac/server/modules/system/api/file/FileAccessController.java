package cn.rbac.server.modules.system.api.file;

import cn.rbac.server.framework.storage.FileContentTypes;
import cn.rbac.server.framework.storage.FileStorageProperties;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        int q = relative.indexOf('?');
        if (q >= 0) {
            relative = relative.substring(0, q);
        }
        int hash = relative.indexOf('#');
        if (hash >= 0) {
            relative = relative.substring(0, hash);
        }
        String basePath = fileStorageProperties.getLocalPath();
        Path base = Paths.get(basePath).normalize().toAbsolutePath();
        Path full = base.resolve(relative).normalize();
        if (!full.startsWith(base) || !Files.exists(full)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        byte[] bytes = Files.readAllBytes(full);
        String storageName = full.getFileName().toString();
        String probed = Files.probeContentType(full);
        String contentType = FileContentTypes.resolve(storageName, null, probed);

        String filenameParam = request.getParameter("filename");
        String dispositionParam = request.getParameter("disposition");
        boolean forceAttachment = "attachment".equalsIgnoreCase(dispositionParam)
                || FileContentTypes.shouldForceDownload(contentType);

        var builder = ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, contentType);
        if (forceAttachment) {
            String downloadName = StringUtils.hasText(filenameParam) ? filenameParam : storageName;
            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(downloadName, StandardCharsets.UTF_8)
                    .build();
            builder.header(HttpHeaders.CONTENT_DISPOSITION, cd.toString());
        }
        return builder.body(bytes);
    }
}
