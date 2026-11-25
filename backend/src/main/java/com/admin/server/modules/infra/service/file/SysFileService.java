package com.admin.server.modules.infra.service.file;

import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.modules.infra.dal.dataobject.file.SysFileDO;
import com.baomidou.mybatisplus.spring.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface SysFileService extends IService<SysFileDO> {

    PageResult<SysFileDO> pageByGroup(Integer pageNo, Integer pageSize, Long groupId, Boolean ungrouped,
                                      String fileCategory, String originalName);

    SysFileDO upload(MultipartFile file, String path, Long groupId);

    /** 按 ID 获取文件并校验当前用户的数据范围；无权限时抛 403 */
    SysFileDO getAccessibleById(Long id);

    SysFileDO uploadImage(MultipartFile file);

    SysFileDO uploadChatImage(MultipartFile file);

    SysFileDO uploadChatFile(MultipartFile file);

    /** AI 对话资料上传：仅校验大小与文件名，不套用文件中心扩展名白名单（允许代码/Markdown 等资料） */
    SysFileDO uploadAiChatFile(MultipartFile file);

    /** AI 知识库文档上传：限定 pdf/doc/docx/md/txt 与单文件大小，原文入文件中心便于预览与追溯 */
    SysFileDO uploadAiKnowledgeFile(MultipartFile file, Long knowledgeBaseId);

    byte[] getFileBytes(Long id);

    /**
     * 系统内部读取文件字节：不做当前用户数据范围校验，仅供异步任务（如知识库解析）使用。
     * <p>禁止暴露为 HTTP 接口，调用方必须在写入前自行确认业务归属。</p>
     */
    byte[] getFileBytesInternal(Long id);

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
