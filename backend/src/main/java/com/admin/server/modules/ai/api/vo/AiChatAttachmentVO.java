package com.admin.server.modules.ai.api.vo;

import lombok.Data;

/**
 * AI 对话附件上传结果 / 历史回放元数据
 */
@Data
public class AiChatAttachmentVO {
    /** 附件ID（服务端生成，发送对话时回传） */
    private String attachmentId;
    /** 原始文件名 */
    private String fileName;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 格式化文件大小，如 1.2 MB */
    private String fileSizeText;
    /** 文件扩展名（小写，不含点） */
    private String fileType;
    /** 是否图片 */
    private Boolean isImage;
    /** 图片内联地址（data URL，仅本轮多模态输入与即时预览使用，不落库） */
    private String imageUrl;
    /** 持久化文件访问地址（历史回放展示/下载） */
    private String fileUrl;
    /** 正文预览（前 200 字） */
    private String previewText;
    /** 附件上下文块（正文片段，供前端保存并在后续轮次回放；不落库） */
    private String contextText;
    /** 解析状态 SUCCESS / FAILED */
    private String parseStatus;
    /** 解析失败原因 */
    private String errorMessage;
}
