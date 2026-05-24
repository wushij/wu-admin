package com.admin.server.modules.message.service;

/**
 * 聊天消息类型：1文本 2图片 3文件 4系统 5已撤回
 */
public final class ChatMsgType {

    public static final int TEXT = 1;
    public static final int IMAGE = 2;
    public static final int FILE = 3;
    public static final int SYSTEM = 4;
    public static final int RECALLED = 5;

    private ChatMsgType() {
    }

    public static String previewLabel(Integer msgType, String content) {
        if (msgType == null) {
            return content;
        }
        return switch (msgType) {
            case IMAGE -> "[图片]";
            case FILE -> filePreview(content);
            case RECALLED -> "[撤回了一条消息]";
            case SYSTEM -> content;
            default -> content;
        };
    }

    private static String filePreview(String content) {
        if (content == null || content.isBlank()) {
            return "[文件]";
        }
        try {
            if (content.trim().startsWith("{")) {
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(content);
                String name = node.path("name").asText(null);
                if (name != null && !name.isBlank()) {
                    return "[文件] " + name;
                }
            }
        } catch (Exception ignored) {
        }
        return "[文件]";
    }
}
