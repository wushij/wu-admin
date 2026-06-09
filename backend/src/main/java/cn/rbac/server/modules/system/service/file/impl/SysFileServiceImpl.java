package cn.rbac.server.modules.system.service.file.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.framework.storage.FileContentTypes;
import cn.rbac.server.framework.storage.LocalFileStorage;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import cn.rbac.server.modules.system.dal.mysql.file.SysFileMapper;
import cn.rbac.server.modules.system.service.file.SysFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFileDO> implements SysFileService {

    /** 企业IM 聊天图片专用目录，文件列表默认不展示 */
    public static final String CHAT_IMAGE_PATH_PREFIX = "images/chat/";
    public static final String CHAT_FILE_PATH_PREFIX = "files/chat/";

    @Resource
    private LocalFileStorage localFileStorage;

    @Value("${app.job.file-recycle-retention-days:30}")
    private int fileRecycleRetentionDays;

    @Override
    public PageResult<SysFileDO> pageByGroup(Integer pageNo, Integer pageSize, Long groupId, Boolean ungrouped,
                                             String fileCategory, String originalName) {
        Page<SysFileDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysFileDO> wrapper = buildGroupListQuery(groupId, ungrouped, fileCategory, null);
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
        excludeChatInternalFiles(wrapper);
        if (Boolean.TRUE.equals(ungrouped)) {
            wrapper.isNull(SysFileDO::getGroupId);
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
        if (!StringUtils.hasText(fileCategory)) {
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
        localFileStorage.validateUpload(file.getOriginalFilename(), file.getSize());
        String originalName = file.getOriginalFilename();
        String suffix = LocalFileStorage.getSuffix(originalName);
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String storagePath = StringUtils.hasText(path) ? path : generatePath();
        try {
            String url = localFileStorage.upload(file.getInputStream(), storagePath, fileName);
            SysFileDO record = new SysFileDO();
            record.setOriginalName(originalName);
            record.setFileName(fileName);
            record.setFilePath(storagePath + "/" + fileName);
            record.setUrl(url);
            record.setFileSize(file.getSize());
            record.setFileType(FileContentTypes.resolve(originalName, file.getContentType(), null));
            record.setFileSuffix(suffix);
            record.setStorageType("local");
            record.setGroupId(groupId);
            record.setCreateBy(SecurityUtils.getLoginUsername());
            record.setCreateTime(LocalDateTime.now());
            save(record);
            return record;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileDO uploadImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("请上传图片文件");
        }
        return upload(file, "images/" + generatePath(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileDO uploadChatImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("请上传图片文件");
        }
        return upload(file, CHAT_IMAGE_PATH_PREFIX + generatePath(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileDO uploadChatFile(MultipartFile file) {
        localFileStorage.validateUpload(file.getOriginalFilename(), file.getSize());
        return upload(file, CHAT_FILE_PATH_PREFIX + generatePath(), null);
    }

    private void excludeChatInternalFiles(LambdaQueryWrapper<SysFileDO> wrapper) {
        wrapper.notLikeRight(SysFileDO::getFilePath, CHAT_IMAGE_PATH_PREFIX)
                .notLikeRight(SysFileDO::getFilePath, CHAT_FILE_PATH_PREFIX);
    }

    @Override
    public byte[] getFileBytes(Long id) {
        SysFileDO record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        try {
            return localFileStorage.readBytes(record.getFilePath());
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败", e);
        }
    }

    @Override
    public org.springframework.core.io.Resource openFileResource(Long id) throws IOException {
        SysFileDO record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        Path path = localFileStorage.resolvePath(record.getFilePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(404, "文件不存在");
        }
        return new FileSystemResource(Objects.requireNonNull(path.toAbsolutePath(), "file path"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysFileDO record = getById(id);
        if (record == null) {
            return;
        }
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
        update(null, new LambdaUpdateWrapper<SysFileDO>()
                .in(SysFileDO::getId, Arrays.asList(fileIds))
                .set(SysFileDO::getGroupId, groupId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, String newName) {
        SysFileDO record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        record.setOriginalName(newName);
        updateById(record);
    }

    private String generatePath() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    @Override
    public PageResult<SysFileDO> recyclePage(PageParam pageParam, String originalName) {
        Page<SysFileDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<SysFileDO> deletedPage = (Page<SysFileDO>) baseMapper.selectDeletedPage(page, originalName);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        SysFileDO record = baseMapper.selectDeletedById(id);
        if (record == null) {
            throw new BusinessException(404, "回收站文件不存在");
        }
        Path path = localFileStorage.resolvePath(record.getFilePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(400, "磁盘文件已不存在，无法恢复");
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
        try {
            localFileStorage.delete(record.getFilePath());
        } catch (IOException ignored) {
            // 磁盘文件可能已不存在，仍清除库记录
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
            deletePermanent(id);
        }
    }
}
