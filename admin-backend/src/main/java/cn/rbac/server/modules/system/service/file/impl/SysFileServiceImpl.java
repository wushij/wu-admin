package cn.rbac.server.modules.system.service.file.impl;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.framework.storage.LocalFileStorage;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import cn.rbac.server.modules.system.dal.mysql.file.SysFileMapper;
import cn.rbac.server.modules.system.service.file.SysFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFileDO> implements SysFileService {

    @Resource
    private LocalFileStorage localFileStorage;

    @Override
    public PageResult<SysFileDO> pageByGroup(Integer pageNo, Integer pageSize, Long groupId, Boolean ungrouped,
                                             String fileCategory, String originalName) {
        Page<SysFileDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysFileDO> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(ungrouped)) {
            wrapper.isNull(SysFileDO::getGroupId);
        } else if (groupId != null) {
            wrapper.eq(SysFileDO::getGroupId, groupId);
        }
        if (StringUtils.hasText(fileCategory)) {
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
        if (StringUtils.hasText(originalName)) {
            wrapper.like(SysFileDO::getOriginalName, originalName);
        }
        wrapper.orderByDesc(SysFileDO::getCreateTime);
        Page<SysFileDO> result = page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
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
            record.setFileType(file.getContentType());
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
            throw new IllegalArgumentException("请上传图片文件");
        }
        return upload(file, "images/" + generatePath(), null);
    }

    @Override
    public byte[] getFileBytes(Long id) {
        SysFileDO record = getById(id);
        if (record == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        try {
            return localFileStorage.readBytes(record.getFilePath());
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysFileDO record = getById(id);
        if (record == null) {
            return;
        }
        try {
            localFileStorage.delete(record.getFilePath());
        } catch (IOException e) {
            // 仍删除库记录
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
            throw new IllegalArgumentException("文件不存在");
        }
        record.setOriginalName(newName);
        updateById(record);
    }

    private String generatePath() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }
}
