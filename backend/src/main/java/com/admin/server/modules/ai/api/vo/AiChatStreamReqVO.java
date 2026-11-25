package com.admin.server.modules.ai.api.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 流式对话入参
 */
@Data
public class AiChatStreamReqVO {
    /** 会话ID（前端生成 UUID，串联多轮对话） */
    private String conversationId;
    /** 指定模型配置ID，为空时使用系统默认模型 */
    private Long modelId;
    /** 来源终端 pc/mobile */
    private String source;
    /** 是否开启联网实时检索 */
    private Boolean webSearch;
    /** 多轮消息上下文（含本轮用户提问，按时间正序） */
    private List<ChatMessage> messages;

    @Data
    public static class ChatMessage {
        /** 角色 user/assistant */
        private String role;
        /** 消息内容 */
        private String content;
        /** 本轮附件ID（仅用户消息携带，服务端据此读取解析正文） */
        private List<String> attachmentIds;
        /** 图片内联地址（data URL，仅用户消息携带，用于多模态视觉输入） */
        private List<String> images;
        /** 附件展示元数据（随对话落库，用于历史回放） */
        private List<AttachmentMeta> attachments;
    }

    /**
     * 附件展示元数据（不包含正文，避免请求体膨胀）
     */
    @Data
    public static class AttachmentMeta {
        /** 附件ID */
        private String attachmentId;
        /** 文件名 */
        private String fileName;
        /** 文件大小（字节） */
        private Long fileSize;
        /** 文件扩展名 */
        private String fileType;
        /** 持久化访问地址 */
        private String fileUrl;
        /** 是否图片 */
        private Boolean isImage;
    }
}
