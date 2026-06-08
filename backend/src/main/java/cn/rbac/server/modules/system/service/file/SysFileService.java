package cn.rbac.server.modules.system.service.file;

import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface SysFileService extends IService<SysFileDO> {

    PageResult<SysFileDO> pageByGroup(Integer pageNo, Integer pageSize, Long groupId, Boolean ungrouped,
                                      String fileCategory, String originalName);

    SysFileDO upload(MultipartFile file, String path, Long groupId);

    SysFileDO uploadImage(MultipartFile file);

    SysFileDO uploadChatImage(MultipartFile file);

    SysFileDO uploadChatFile(MultipartFile file);

    byte[] getFileBytes(Long id);

    /** 本地文件 Resource（支持 HTTP Range 流式读取，用于预览/下载） */
    org.springframework.core.io.Resource openFileResource(Long id) throws IOException;

    void delete(Long id);

    void deleteBatch(Long[] ids);

    void moveToGroup(Long[] fileIds, Long groupId);

    void rename(Long id, String newName);

    /** 分组侧栏数量（与文件列表相同的分类、排除聊天目录） */
    long countForGroupSidebar(Long groupId, Boolean ungrouped, String fileCategory);

    PageResult<SysFileDO> recyclePage(PageParam pageParam, String originalName);

    void restore(Long id);

    void deletePermanent(Long id);

    void purgeExpiredRecycleBin();
}
