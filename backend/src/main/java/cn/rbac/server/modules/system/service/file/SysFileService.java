package cn.rbac.server.modules.system.service.file;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface SysFileService extends IService<SysFileDO> {

    PageResult<SysFileDO> pageByGroup(Integer pageNo, Integer pageSize, Long groupId, Boolean ungrouped,
                                      String fileCategory, String originalName);

    SysFileDO upload(MultipartFile file, String path, Long groupId);

    SysFileDO uploadImage(MultipartFile file);

    byte[] getFileBytes(Long id);

    void delete(Long id);

    void deleteBatch(Long[] ids);

    void moveToGroup(Long[] fileIds, Long groupId);

    void rename(Long id, String newName);
}
