package com.admin.server.framework.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
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
 * 阿里云 OSS 对象存储驱动实现 (基于 S3 兼容协议通道)
 */
@Slf4j
public class AliyunOssFileStorageService implements FileStorageService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration IO_TIMEOUT = Duration.ofSeconds(30);

    private final MinioClient ossClient;
    private final String defaultBucket;
    private final String endpoint;
    private final String customDomain;

    public AliyunOssFileStorageService(String endpoint, String accessKeyId, String accessKeySecret, String bucket, String customDomain) {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("阿里云 OSS Endpoint 未配置 (例如 oss-cn-hangzhou.aliyuncs.com)");
        }
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            throw new IllegalArgumentException("阿里云 OSS 访问密钥未配置 (AccessKeyId / AccessKeySecret)");
        }

        this.defaultBucket = StringUtils.hasText(bucket) ? bucket.trim() : "wu-admin";
        String normalizedEndpoint = endpoint.trim();
        if (!normalizedEndpoint.startsWith("http://") && !normalizedEndpoint.startsWith("https://")) {
            normalizedEndpoint = "https://" + normalizedEndpoint;
        }
        this.endpoint = normalizedEndpoint;
        this.customDomain = StringUtils.hasText(customDomain) ? customDomain.trim().replaceAll("/+$", "") : null;

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(IO_TIMEOUT)
                .writeTimeout(IO_TIMEOUT)
                .build();

        this.ossClient = MinioClient.builder()
                .endpoint(this.endpoint)
                .credentials(accessKeyId.trim(), accessKeySecret.trim())
                .httpClient(httpClient)
                .build();
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            ossClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(cleanObjectName)
                            .stream(inputStream, -1L, 10485760L)
                            .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                            .build()
            );
            return buildUrl(bucket, cleanObjectName);
        } catch (Exception e) {
            log.error("阿里云 OSS 上传失败: {}", describeException(e), e);
            throw new IllegalStateException("阿里云 OSS 上传失败: " + describeException(e), e);
        }
    }

    @Override
    public InputStream getFile(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            return ossClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(cleanObjectName).build()
            );
        } catch (Exception e) {
            log.warn("阿里云 OSS 获取文件流失败: bucket={}, objectName={}, reason={}", bucket, cleanObjectName, describeException(e));
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
            throw new IllegalStateException("读取阿里云 OSS 文件字节失败: " + e.getMessage(), e);
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
            return ossClient.getObject(builder.build());
        } catch (Exception e) {
            log.warn("阿里云 OSS 获取文件区间失败: bucket={}, objectName={}, offset={}, length={}, reason={}",
                    bucket, cleanObjectName, offset, length, describeException(e));
            return null;
        }
    }

    @Override
    public long getFileSize(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            return ossClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(cleanObjectName)
                    .build()).size();
        } catch (Exception e) {
            log.debug("阿里云 OSS 读取文件大小失败: bucket={}, objectName={}, reason={}",
                    bucket, cleanObjectName, describeException(e));
            return -1L;
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : defaultBucket;
        String cleanObjectName = normalizeObjectName(objectName);
        try {
            ossClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(cleanObjectName).build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("阿里云 OSS 删除文件失败: " + describeException(e), e);
        }
    }

    @Override
    public String getStorageType() {
        return "oss";
    }

    @Override
    public boolean testConnection() {
        return testConnectionDetail() == null;
    }

    @Override
    public String testConnectionDetail() {
        try {
            boolean exists = ossClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucket).build());
            return exists ? null : "指定的阿里云 OSS 存储空间(Bucket)不存在或无访问权限: " + defaultBucket;
        } catch (Exception e) {
            log.warn("阿里云 OSS 连通性测试失败: {}", describeException(e));
            return describeException(e);
        }
    }

    private String buildUrl(String bucket, String objectName) {
        if (StringUtils.hasText(customDomain)) {
            return customDomain + "/" + objectName;
        }
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
            return "OSS错误响应: code=" + code + ", message=" + message;
        }
        if (ex instanceof java.io.IOException || ex.getCause() instanceof java.io.IOException) {
            Throwable root = ex.getCause() != null ? ex.getCause() : ex;
            return "网络不可达或超时（请检查 endpoint 与网络配置）: " + root.getMessage();
        }
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
