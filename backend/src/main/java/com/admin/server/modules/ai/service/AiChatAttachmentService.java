package com.admin.server.modules.ai.service;

import com.admin.server.modules.ai.api.vo.AiChatAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI wu 助手对话附件服务：上传落盘、正文解析、上下文组装。
 */
public interface AiChatAttachmentService {

    /** 单次提问最多可携带的附件数 */
    int MAX_ATTACHMENTS = 5;

    /** 上传并解析附件，返回展示与上下文所需元数据 */
    AiChatAttachmentVO uploadAndParse(MultipartFile file);

    /** 按附件ID组装注入模型的正文上下文（图片返回多模态说明，文档返回正文） */
    String buildContext(List<String> attachmentIds);

    /** 按附件ID取图片内联地址（data URL），用于多模态视觉输入 */
    List<String> resolveImageUrls(List<String> attachmentIds);
}
