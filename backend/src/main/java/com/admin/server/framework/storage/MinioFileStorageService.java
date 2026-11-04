package com.admin.server.framework.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.Duration;

/**
 * MinIO 私有化对象存储驱动实现
 */
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration IO_TIMEOUT = Duration.ofSeconds(30);

    private final MinioClient minioClient;
    private final String defaultBucket;
    private final String endpoint;
    private final String customDomain;

    public MinioFileStorageService(String endpoint, String accessKey, String secretKey, String bucket, String customDomain) {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("MinIO 服务端点未配置 (如 http://127.0.0.1:9000)");
        }
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("MinIO 访问密钥未配置 (AccessKey / SecretKey)");
        }

        this.defaultBucket = StringUtils.hasText(bucket) ? bucket.trim() : "wu-admin";
        this.endpoint = endpoint.trim();
        this.customDomain = StringUtils.hasText(customDomain) ? customDomain.trim().replaceAll("/+$", "") : null;

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(IO_TIMEOUT)
                .writeTimeout(IO_TIMEOUT)
                .build();

        this.minioClient = MinioClient.builder()
                .endpoint(this.endpoint)
                .credentials(accessKey.trim(), secretKey.trim())
                .httpClient(httpClient)
                .build();
    }

    private void ensureBucketAvailable(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO 存储桶不存在，已自动创建: bucket={}", bucket);
        }
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            ensureBucketAvailable(bucket);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(cleanObjectName)
                            .stream(inputStream, -1L, 10485760L)
                            .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                            .build()
            );
            return buildUrl(bucket, cleanObjectName);
        } catch (Exception e) {
            log.error("MinIO 文件上传失败: {}", describeException(e), e);
            throw new IllegalStateException("MinIO 文件上传失败: " + describeException(e), e);
        }
    }

    @Override
    public InputStream getFile(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(cleanObjectName).build()
            );
        } catch (Exception e) {
            log.warn("MinIO 获取文件流失败: bucket={}, objectName={}, reason={}", bucket, cleanObjectName, describeException(e));
            return null;
        }
    }

    @Override
    public byte[] getFileBytes(String bucketName, String objectName) {
        try (InputStream in = getFile(bucketName, objectName)) {
            if (in == null) {
                return new byte[0];
            }
            return StreamUtils.copyToByteArray(in);
        } catch (Exception e) {
            throw new IllegalStateException("读取 MinIO 文件字节失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getFile(String bucketName, String objectName, long offset, long length) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            GetObjectArgs.Builder builder = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(cleanObjectName)
                    .offset(Math.max(0L, offset));
            if (length > 0) {
                builder.length(length);
            }
            return minioClient.getObject(builder.build());
        } catch (Exception e) {
            log.warn("MinIO 获取文件区间失败: bucket={}, objectName={}, offset={}, length={}, reason={}",
                    bucket, cleanObjectName, offset, length, describeException(e));
            return null;
        }
    }

    @Override
    public long getFileSize(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(cleanObjectName)
                    .build()).size();
        } catch (Exception e) {
            log.debug("MinIO 读取文件大小失败: bucket={}, objectName={}, reason={}",
                    bucket, cleanObjectName, describeException(e));
            return -1L;
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(cleanObjectName).build()
            );
        } catch (Exception e) {
            // 删除失败不能让上层误以为已经清理成功
            throw new IllegalStateException("MinIO 删除文件失败: " + describeException(e), e);
        }
    }

    @Override
    public String getStorageType() {
        return "minio";
    }

    @Override
    public boolean testConnection() {
        return testConnectionDetail() == null;
    }

    @Override
    public String testConnectionDetail() {
        try {
            ensureBucketAvailable(defaultBucket);
            return null;
        } catch (Exception e) {
            log.warn("MinIO 连通性测试失败: {}", describeException(e));
            return describeException(e);
        }
    }

    private String buildUrl(String bucket, String objectName) {
        if (StringUtils.hasText(customDomain)) {
            return customDomain + "/" + bucket + "/" + objectName;
        }
        // 默认统一走后端流式代理：私有桶、内网 endpoint 构建出的裸地址浏览器无法直接访问
        return StorageObjectKeyBuilder.storageFileUrl(bucket, objectName);
    }

    private String normalizeObjectName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new IllegalArgumentException("对象名称不能为空");
        }
        String clean = objectName.replace("\\", "/");
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        return clean;
    }

    private String describeException(Exception ex) {
        if (ex instanceof ErrorResponseException ere) {
            String code = ere.errorResponse() != null ? ere.errorResponse().code() : null;
            String message = ere.errorResponse() != null ? ere.errorResponse().message() : null;
            return "MinIO错误响应: code=" + code + ", message=" + message;
        }
        if (ex instanceof java.io.IOException || ex.getCause() instanceof java.io.IOException) {
            Throwable root = ex.getCause() != null ? ex.getCause() : ex;
            return "网络不可达或超时（请检查 endpoint 与端口防火墙）: " + root.getMessage();
        }
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
