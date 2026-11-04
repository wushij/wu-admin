package com.admin.server.modules.infra.service.file.impl;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.framework.storage.FileContentTypes;
import com.admin.server.framework.storage.LocalFileStorage;
import com.admin.server.framework.storage.StorageObjectKeyBuilder;
import com.admin.server.modules.infra.dal.dataobject.file.SysFileDO;
import com.admin.server.modules.infra.dal.mysql.file.SysFileMapper;
import com.admin.server.modules.infra.service.file.SysFileService;
import com.admin.server.modules.system.service.permission.DataScopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFileDO> implements SysFileService {

    /** 聊天图片专用目录，文件列表默认不展示 */
    public static final String CHAT_IMAGE_PATH_PREFIX = "images/chat/";
    public static final String CHAT_FILE_PATH_PREFIX = "files/chat/";
    /** 新版聊天附件统一前缀：chat/images/、chat/files/ */
    public static final String CHAT_ROOT_PATH_PREFIX = "chat/";

    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");
    private static final int MAX_ORIGINAL_NAME_LENGTH = 180;
    private static final int MAX_FILE_PATH_LENGTH = 500;
    private static final int MAX_URL_LENGTH = 1000;
    private static final int MAX_FILE_TYPE_LENGTH = 100;
    private static final int MAX_SUFFIX_LENGTH = 32;

    @Resource
    private LocalFileStorage localFileStorage;

    @Resource
    private com.admin.server.framework.storage.RoutingFileStorageService routingFileStorageService;

    @Resource
    private DataScopeService dataScopeService;

    @Resource
    private com.admin.server.modules.infra.dal.mysql.file.SysFileGroupMapper fileGroupMapper;

    @Value("${app.job.file-recycle-retention-days:30}")
    private int fileRecycleRetentionDays;

    @Override
    public SysFileDO getAccessibleById(Long id) {
        return requireVisibleFile(id);
    }

    @Override
    public PageResult<SysFileDO> pageByGroup(Integer pageNo, Integer pageSize, Long groupId, Boolean ungrouped,
                                             String fileCategory, String originalName) {
        Page<SysFileDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysFileDO> wrapper = buildGroupListQuery(groupId, ungrouped, fileCategory, originalName);
        wrapper.orderByDesc(SysFileDO::getCreateTime);
        Page<SysFileDO> result = page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public long countForGroupSidebar(Long groupId, Boolean ungrouped, String fileCategory) {
        return count(buildGroupListQuery(groupId, ungrouped, fileCategory, null));
    }

    private LambdaQueryWrapper<SysFileDO> buildGroupListQuery(Long groupId, Boolean ungrouped,
                                                              String fileCategory, String originalName) {
        LambdaQueryWrapper<SysFileDO> wrapper = new LambdaQueryWrapper<>();
        // 组织隔离：仅展示当前角色数据范围内的文件
        DataScopeService.DataScopeContext scope = dataScopeService.getCurrentScope();
        if (!scope.all()) {
            dataScopeService.applyDeptIdScope(wrapper, SysFileDO::getDeptId, scope);
            if (scope.selfOnly()) {
                String username = SecurityUtils.getLoginUsername();
                wrapper.eq(SysFileDO::getCreateBy, StringUtils.hasText(username) ? username : "__none__");
            }
        }
        excludeChatInternalFiles(wrapper);
        if (Boolean.TRUE.equals(ungrouped)) {
            // 兼容历史数据中错误写入的 group_id = 0
            wrapper.and(w -> w.isNull(SysFileDO::getGroupId).or().eq(SysFileDO::getGroupId, 0L));
        } else if (groupId != null) {
            wrapper.eq(SysFileDO::getGroupId, groupId);
        }
        applyFileCategory(wrapper, fileCategory);
        if (StringUtils.hasText(originalName)) {
            wrapper.like(SysFileDO::getOriginalName, originalName);
        }
        return wrapper;
    }

    private void applyFileCategory(LambdaQueryWrapper<SysFileDO> wrapper, String fileCategory) {
        if (!StringUtils.hasText(fileCategory) || "all".equalsIgnoreCase(fileCategory)) {
            return;
        }
        if ("image".equals(fileCategory)) {
            wrapper.likeRight(SysFileDO::getFileType, "image/");
        } else if ("video".equals(fileCategory)) {
            wrapper.likeRight(SysFileDO::getFileType, "video/");
        } else if ("audio".equals(fileCategory)) {
            wrapper.likeRight(SysFileDO::getFileType, "audio/");
        } else if ("other".equals(fileCategory)) {
            wrapper.and(w -> w
                    .notLike(SysFileDO::getFileType, "image/")
                    .notLike(SysFileDO::getFileType, "video/")
                    .notLike(SysFileDO::getFileType, "audio/"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileDO upload(MultipartFile file, String path, Long groupId) {
        requireVisibleGroup(groupId);
        localFileStorage.validateUpload(file.getOriginalFilename(), file.getSize());
        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String objectName;
        if (StringUtils.hasText(path)) {
            String cleanPath = normalizeObjectPath(path);
            String fileName = StorageObjectKeyBuilder.shortHash() + "_"
                    + StorageObjectKeyBuilder.sanitizeFilename(originalName);
            objectName = cleanPath.endsWith("/") ? cleanPath + fileName : cleanPath + "/" + fileName;
        } else {
            objectName = StorageObjectKeyBuilder.sysFile(null, originalName);
        }
        return uploadAndPersist(file, originalName, objectName, groupId, dataScopeService.getCurrentDeptId());
    }

    private String resolveActiveBucket(String storageType) {
        com.admin.server.framework.storage.StorageConfig cfg = routingFileStorageService.getCurrentConfig();
        if (cfg == null) return "wu-admin";
        return switch (storageType.toLowerCase()) {
            case "minio" -> StringUtils.hasText(cfg.getMinioBucket()) ? cfg.getMinioBucket() : "wu-admin";
            case "cos" -> StringUtils.hasText(cfg.getCosBucket()) ? cfg.getCosBucket() : "wu-admin";
            case "oss" -> StringUtils.hasText(cfg.getOssBucket()) ? cfg.getOssBucket() : "wu-admin";
            default -> "wu-admin";
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileDO uploadImage(MultipartFile file) {
        validateImageUpload(file);
        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String objectName = StorageObjectKeyBuilder.sysFile("images", originalName);
        return uploadAndPersist(file, originalName, objectName, null, dataScopeService.getCurrentDeptId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileDO uploadChatImage(MultipartFile file) {
        validateImageUpload(file);
        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String objectName = StorageObjectKeyBuilder.chatImage(originalName);
        return uploadAndPersist(file, originalName, objectName, null, dataScopeService.getCurrentDeptId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileDO uploadChatFile(MultipartFile file) {
        localFileStorage.validateUpload(file.getOriginalFilename(), file.getSize());
        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String objectName = StorageObjectKeyBuilder.chatFile(originalName);
        return uploadAndPersist(file, originalName, objectName, null, dataScopeService.getCurrentDeptId());
    }

    private SysFileDO uploadAndPersist(MultipartFile file, String originalName, String objectName,
                                       Long groupId, Long deptId) {
        validateObjectNameLength(objectName);
        String suffix = normalizeSuffix(LocalFileStorage.getSuffix(originalName));
        String fileName = fileNameOf(objectName);
        String storageType = routingFileStorageService.getStorageType();
        String bucketName = resolveActiveBucket(storageType);
        String fileType = normalizeFileType(
                FileContentTypes.resolve(originalName, file.getContentType(), null));

        boolean uploaded = false;
        try {
            // 先标记已触达存储，后续任何失败都尝试补偿删除，避免本地半截文件或对象存储孤儿对象
            uploaded = true;
            String url;
            try (InputStream in = file.getInputStream()) {
                url = routingFileStorageService.uploadFile(
                        bucketName, objectName, in, file.getContentType());
            }

            SysFileDO record = new SysFileDO();
            record.setDeptId(deptId);
            record.setOriginalName(originalName);
            record.setFileName(fileName);
            record.setFilePath(objectName);
            record.setUrl(url);
            record.setFileSize(file.getSize());
            record.setFileType(fileType);
            record.setFileSuffix(suffix);
            record.setStorageType(storageType);
            record.setBucketName(bucketName);
            record.setGroupId(groupId != null && groupId > 0 ? groupId : null);
            record.setCreateBy(SecurityUtils.getLoginUsername());
            record.setCreateTime(LocalDateTime.now());
            validateRecordLengths(record);
            save(record);
            return record;
        } catch (IOException e) {
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        } catch (RuntimeException e) {
            if (uploaded) {
                try {
                    routingFileStorageService.deleteFileForRecord(storageType, bucketName, objectName);
                } catch (Exception cleanupError) {
                    log.warn("文件写入成功但数据库保存失败，补偿删除存储对象也失败: bucket={}, object={}, reason={}",
                            bucketName, objectName, cleanupError.getMessage());
                }
            }
            throw e;
        }
    }

    private void validateImageUpload(MultipartFile file) {
        localFileStorage.validateUpload(file.getOriginalFilename(), file.getSize());
        String extension = LocalFileStorage.getSuffix(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "图片仅支持 jpg、jpeg、png、gif、webp、bmp 格式");
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)
                && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(400, "请上传图片文件");
        }
    }

    private String normalizeOriginalName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "file";
        }
        String clean = originalName.replace("\\", "/");
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        clean = clean.replaceAll("[\\r\\n\\t]", " ").trim();
        if (!StringUtils.hasText(clean)) {
            return "file";
        }
        if (clean.length() > MAX_ORIGINAL_NAME_LENGTH) {
            String suffix = normalizeSuffix(LocalFileStorage.getSuffix(clean));
            int keep = Math.max(1, MAX_ORIGINAL_NAME_LENGTH - suffix.length());
            clean = clean.substring(0, Math.min(clean.length(), keep)) + suffix;
        }
        return clean;
    }

    private String normalizeObjectPath(String path) {
        String clean = path.trim().replace("\\", "/");
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (clean.contains("..")) {
            throw new BusinessException(400, "非法的存储路径");
        }
        String lower = clean.toLowerCase(Locale.ROOT);
        if ("public".equals(lower) || lower.startsWith("public/")) {
            throw new BusinessException(400, "不允许上传到公开目录");
        }
        if (!StringUtils.hasText(clean)) {
            throw new BusinessException(400, "存储路径不能为空");
        }
        return clean.endsWith("/") ? clean.substring(0, clean.length() - 1) : clean;
    }

    private String normalizeSuffix(String suffix) {
        if (!StringUtils.hasText(suffix)) {
            return "";
        }
        String clean = suffix.toLowerCase(Locale.ROOT);
        return clean.length() <= MAX_SUFFIX_LENGTH ? clean : "";
    }

    private String normalizeFileType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return "application/octet-stream";
        }
        return fileType.length() <= MAX_FILE_TYPE_LENGTH
                ? fileType
                : fileType.substring(0, MAX_FILE_TYPE_LENGTH);
    }

    private String fileNameOf(String objectName) {
        int lastSlash = objectName.lastIndexOf('/');
        return lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
    }

    private void validateObjectNameLength(String objectName) {
        if (!StringUtils.hasText(objectName) || objectName.length() > MAX_FILE_PATH_LENGTH) {
            throw new BusinessException(400, "文件存储路径过长，请缩短文件名");
        }
    }

    private void validateRecordLengths(SysFileDO record) {
        if (record.getOriginalName() != null && record.getOriginalName().length() > 255) {
            throw new BusinessException(400, "文件名过长");
        }
        if (record.getFilePath() == null || record.getFilePath().length() > MAX_FILE_PATH_LENGTH) {
            throw new BusinessException(400, "文件存储路径过长");
        }
        if (record.getUrl() == null || record.getUrl().length() > MAX_URL_LENGTH) {
            throw new BusinessException(400, "文件访问地址过长");
        }
        if (record.getFileType() != null && record.getFileType().length() > MAX_FILE_TYPE_LENGTH) {
            throw new BusinessException(400, "文件类型过长");
        }
        if (record.getFileSuffix() != null && record.getFileSuffix().length() > MAX_SUFFIX_LENGTH) {
            throw new BusinessException(400, "文件后缀过长");
        }
        if (record.getStorageType() != null && record.getStorageType().length() > 20) {
            throw new BusinessException(400, "存储引擎类型过长");
        }
        if (record.getBucketName() != null && record.getBucketName().length() > 100) {
            throw new BusinessException(400, "存储桶名称过长");
        }
    }

    private void excludeChatInternalFiles(LambdaQueryWrapper<SysFileDO> wrapper) {
        // 兼容新旧两套聊天附件前缀：images/chat/、files/chat/ 与 chat/images/、chat/files/
        wrapper.notLikeRight(SysFileDO::getFilePath, CHAT_ROOT_PATH_PREFIX)
                .notLikeRight(SysFileDO::getFilePath, CHAT_IMAGE_PATH_PREFIX)
                .notLikeRight(SysFileDO::getFilePath, CHAT_FILE_PATH_PREFIX);
    }

    /**
     * 校验当前用户是否有权访问指定文件；详情、下载、预览与写操作统一走这里。
     */
    private SysFileDO requireVisibleFile(Long id) {
        SysFileDO record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        if (!isFileVisible(record)) {
            throw new BusinessException(403, "无权访问该文件");
        }
        return record;
    }

    private boolean isFileVisible(SysFileDO record) {
        if (record == null) {
            return false;
        }
        DataScopeService.DataScopeContext scope = dataScopeService.getCurrentScope();
        if (scope.all()) {
            return true;
        }
        if (record.getDeptId() != null && scope.deptIds().contains(record.getDeptId())) {
            return true;
        }
        if (scope.selfOnly()) {
            String username = SecurityUtils.getLoginUsername();
            return StringUtils.hasText(username) && Objects.equals(record.getCreateBy(), username);
        }
        return false;
    }

    private void requireVisibleGroup(Long groupId) {
        if (groupId == null || groupId <= 0) {
            return;
        }
        com.admin.server.modules.infra.dal.dataobject.file.SysFileGroupDO group =
                fileGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(404, "文件分组不存在");
        }
        DataScopeService.DataScopeContext scope = dataScopeService.getCurrentScope();
        if (scope.all()) {
            return;
        }
        if (group.getDeptId() != null && scope.deptIds().contains(group.getDeptId())) {
            return;
        }
        if (scope.selfOnly()) {
            String username = SecurityUtils.getLoginUsername();
            if (StringUtils.hasText(username) && Objects.equals(group.getCreateBy(), username)) {
                return;
            }
        }
        throw new BusinessException(403, "无权操作该文件分组");
    }

    @Override
    public byte[] getFileBytes(Long id) {
        SysFileDO record = requireVisibleFile(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        String storageType = record.getStorageType();
        if ("local".equalsIgnoreCase(storageType) || !StringUtils.hasText(storageType)) {
            try {
                Path path = localFileStorage.resolvePath(record.getFilePath());
                if (Files.isRegularFile(path)) {
                    return Files.readAllBytes(path);
                }
            } catch (Exception ignored) {
            }
        }
        byte[] bytes = routingFileStorageService.getFileBytes(record.getBucketName(), record.getFilePath());
        if (bytes != null && bytes.length > 0) {
            return bytes;
        }
        try {
            return localFileStorage.readBytes(record.getFilePath());
        } catch (IOException e) {
            throw new BusinessException(404, "读取文件字节失败");
        }
    }

    @Override
    public org.springframework.core.io.Resource openFileResource(Long id) throws IOException {
        SysFileDO record = requireVisibleFile(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        String storageType = record.getStorageType();
        if ("local".equalsIgnoreCase(storageType) || !StringUtils.hasText(storageType)) {
            try {
                Path path = localFileStorage.resolvePath(record.getFilePath());
                if (Files.isRegularFile(path)) {
                    return new FileSystemResource(Objects.requireNonNull(path.toAbsolutePath(), "file path"));
                }
            } catch (Exception ignored) {
            }
        }

        InputStream in = routingFileStorageService.getFile(record.getBucketName(), record.getFilePath());
        if (in == null) {
            throw new BusinessException(404, "存储源文件不存在或已损坏");
        }
        long size = record.getFileSize() != null ? record.getFileSize() : -1L;
        if (size < 0) {
            size = routingFileStorageService.getFileSize(record.getBucketName(), record.getFilePath());
        }
        final long contentLength = size;
        String desc = StringUtils.hasText(record.getOriginalName()) ? record.getOriginalName() : record.getFileName();
        return new org.springframework.core.io.InputStreamResource(in, desc) {
            @Override
            public long contentLength() {
                // 不要调用 super.contentLength()：InputStreamResource 会为计算长度消费掉整个流
                return contentLength;
            }

            @Override
            public String getFilename() {
                return record.getOriginalName();
            }
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireVisibleFile(id);
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long[] ids) {
        if (ids == null) {
            return;
        }
        Arrays.stream(ids).forEach(this::delete);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveToGroup(Long[] fileIds, Long groupId) {
        if (fileIds == null || fileIds.length == 0) {
            return;
        }
        // 未分组在前端可能以 0 或 null 表示，数据库统一存 NULL，避免 group_id = 0 后列表查不到
        Long targetGroupId = groupId != null && groupId > 0 ? groupId : null;
        requireVisibleGroup(targetGroupId);
        for (Long fileId : fileIds) {
            requireVisibleFile(fileId);
        }
        update(null, new LambdaUpdateWrapper<SysFileDO>()
                .in(SysFileDO::getId, Arrays.asList(fileIds))
                .set(SysFileDO::getGroupId, targetGroupId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, String newName) {
        if (!StringUtils.hasText(newName)) {
            throw new BusinessException(400, "文件名不能为空");
        }
        SysFileDO record = requireVisibleFile(id);
        String normalizedName = normalizeOriginalName(newName);
        record.setOriginalName(normalizedName);
        record.setFileSuffix(normalizeSuffix(LocalFileStorage.getSuffix(normalizedName)));
        updateById(record);
    }

    @Override
    public PageResult<SysFileDO> recyclePage(PageParam pageParam, String originalName) {
        Page<SysFileDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        DataScopeService.DataScopeContext scope = dataScopeService.getCurrentScope();
        List<Long> deptIds = scope.all() ? null : scope.deptIds();
        String createBy = scope.selfOnly() ? SecurityUtils.getLoginUsername() : null;
        Page<SysFileDO> deletedPage = (Page<SysFileDO>) baseMapper.selectDeletedPage(
                page, originalName, deptIds, createBy);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        SysFileDO record = baseMapper.selectDeletedById(id);
        if (record == null) {
            throw new BusinessException(404, "回收站文件不存在");
        }
        if (!isFileVisible(record)) {
            throw new BusinessException(403, "无权恢复该文件");
        }
        boolean exists = false;
        String storageType = record.getStorageType();
        if ("local".equalsIgnoreCase(storageType) || !StringUtils.hasText(storageType)) {
            try {
                Path path = localFileStorage.resolvePath(record.getFilePath());
                exists = Files.isRegularFile(path);
            } catch (Exception ignored) {
            }
        }
        if (!exists) {
            try (InputStream in = routingFileStorageService.getFile(record.getBucketName(), record.getFilePath())) {
                exists = in != null;
            } catch (Exception ignored) {
            }
        }
        if (!exists) {
            throw new BusinessException(400, "源存储文件已不存在，无法恢复");
        }
        int rows = baseMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站文件不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        SysFileDO record = baseMapper.selectDeletedById(id);
        if (record == null) {
            throw new BusinessException(404, "回收站文件不存在");
        }
        if (!isFileVisible(record)) {
            throw new BusinessException(403, "无权删除该文件");
        }
        try {
            // 按记录中的历史存储引擎删除；本地记录再兜底清理旧路径，避免热切换后删错或漏删
            routingFileStorageService.deleteFileForRecord(
                    record.getStorageType(), record.getBucketName(), record.getFilePath());
            if ("local".equalsIgnoreCase(record.getStorageType())
                    || !StringUtils.hasText(record.getStorageType())) {
                try {
                    localFileStorage.delete(record.getFilePath());
                } catch (Exception legacyError) {
                    log.debug("旧本地路径文件不存在或已清理: {}", legacyError.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("回收站文件物理删除失败: id={}, bucket={}, path={}, reason={}",
                    id, record.getBucketName(), record.getFilePath(), e.getMessage());
            throw new BusinessException(500, "物理文件删除失败，文件记录已保留，请稍后重试");
        }
        int rows = baseMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站文件不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredRecycleBin() {
        if (fileRecycleRetentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(fileRecycleRetentionDays);
        List<Long> ids = baseMapper.selectExpiredRecycleIds(cutoff);
        for (Long id : ids) {
            try {
                deletePermanent(id);
            } catch (Exception e) {
                // 单条失败不阻断其余文件清理，下一次任务会继续重试
                log.warn("回收站过期文件清理失败: id={}, reason={}", id, e.getMessage());
            }
        }
    }
}
