package com.admin.server.framework.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, java.util.Set<WebSocketSession>> ONLINE_SESSIONS = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return;
        }
        boolean isFirstSession = false;
        java.util.Set<WebSocketSession> sessions = ONLINE_SESSIONS.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        synchronized (sessions) {
            if (sessions.isEmpty()) {
                isFirstSession = true;
            }
            sessions.add(session);
        }
        sendJson(session, Map.of("type", "connected", "content", "ok"));
        if (isFirstSession) {
            broadcastPresence(userId, true);
        }
        log.info("WS connected userId={}, userSessions={}, onlineUsers={}", userId, sessions.size(), ONLINE_SESSIONS.size());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        try {
            var node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            if ("ping".equals(type)) {
                sendJson(session, Map.of("type", "pong"));
                return;
            }
            if ("typing".equals(type)) {
                Long fromUserId = getUserId(session);
                long toUserId = node.path("toUserId").asLong(0);
                if (fromUserId != null && toUserId > 0 && !fromUserId.equals(toUserId)) {
                    sendTypingPayload(toUserId, fromUserId);
                }
            }
        } catch (Exception e) {
            log.warn("WS message parse failed", e);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            java.util.Set<WebSocketSession> sessions = ONLINE_SESSIONS.get(userId);
            boolean isLastSession = false;
            if (sessions != null) {
                synchronized (sessions) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        ONLINE_SESSIONS.remove(userId);
                        isLastSession = true;
                    }
                }
            }
            if (isLastSession) {
                broadcastPresence(userId, false);
            }
        }
    }

    public void sendToUser(Long userId, String json) {
        java.util.Set<WebSocketSession> sessions = ONLINE_SESSIONS.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            for (WebSocketSession session : sessions) {
                if (session != null && session.isOpen()) {
                    sendRaw(session, json);
                }
            }
        }
    }

    public void sendNotice(Long userId, Long announceId, String title, String content) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "notice");
            payload.put("title", title != null ? title : "");
            payload.put("content", content != null ? content : "");
            payload.put("time", System.currentTimeMillis());
            if (announceId != null) {
                payload.put("announceId", announceId);
            }
            String json = objectMapper.writeValueAsString(payload);
            if (userId == null) {
                broadcast(json);
            } else {
                sendToUser(userId, json);
            }
        } catch (Exception e) {
            log.error("sendNotice failed", e);
        }
    }

    public void sendChatPayload(Long receiverId, Map<String, Object> payload) {
        try {
            payload.put("type", "chat");
            payload.put("time", System.currentTimeMillis());
            sendToUser(receiverId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("sendChatPayload failed", e);
        }
    }

    public void sendGroupChatPayload(Long userId, Map<String, Object> payload) {
        try {
            if (!payload.containsKey("type")) {
                payload.put("type", "groupChat");
            }
            payload.put("time", System.currentTimeMillis());
            sendToUser(userId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("sendGroupChatPayload failed", e);
        }
    }

    public void sendTypingPayload(Long toUserId, Long fromUserId) {
        sendTypingPayload(toUserId, fromUserId, true);
    }

    /** active=false 时通知对方停止显示「正在输入」 */
    public void sendTypingStopPayload(Long toUserId, Long fromUserId) {
        sendTypingPayload(toUserId, fromUserId, false);
    }

    private void sendTypingPayload(Long toUserId, Long fromUserId, boolean active) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "typing");
            payload.put("fromUserId", fromUserId);
            payload.put("active", active);
            payload.put("time", System.currentTimeMillis());
            sendToUser(toUserId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("sendTypingPayload failed", e);
        }
    }

    public boolean isOnline(Long userId) {
        java.util.Set<WebSocketSession> sessions = ONLINE_SESSIONS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        for (WebSocketSession session : sessions) {
            if (session != null && session.isOpen()) {
                return true;
            }
        }
        return false;
    }

    private void broadcast(String json) {
        ONLINE_SESSIONS.values().forEach(sessions -> {
            if (sessions != null) {
                sessions.forEach(s -> sendRaw(s, json));
            }
        });
    }

    /** 通知其他在线用户：某人上线/下线（企业 IM 联系人列表实时状态） */
    private void broadcastPresence(Long userId, boolean online) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "presence",
                    "userId", userId,
                    "online", online
            ));
            ONLINE_SESSIONS.forEach((uid, sessions) -> {
                if (!uid.equals(userId) && sessions != null) {
                    sessions.forEach(s -> sendRaw(s, json));
                }
            });
        } catch (Exception e) {
            log.warn("broadcastPresence failed userId={}", userId, e);
        }
    }

    private void sendJson(WebSocketSession session, Map<String, ?> map) {
        try {
            sendRaw(session, objectMapper.writeValueAsString(map));
        } catch (Exception e) {
            log.warn("WS sendJson failed userId={}", getUserId(session), e);
        }
    }

    private void sendRaw(WebSocketSession session, String json) {
        if (session != null && session.isOpen() && json != null) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.warn("WS send failed", e);
            }
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object v = session.getAttributes().get("userId");
        if (v instanceof Long l) {
            return l;
        }
        if (v != null) {
            return Long.parseLong(v.toString());
        }
        return null;
    }
}
