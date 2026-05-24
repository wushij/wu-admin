package cn.rbac.server.framework.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> ONLINE_SESSIONS = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return;
        }
        WebSocketSession old = ONLINE_SESSIONS.put(userId, session);
        closeQuietly(old);
        sendJson(session, Map.of("type", "connected", "content", "ok"));
        log.info("WS connected userId={}, online={}", userId, ONLINE_SESSIONS.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            var node = objectMapper.readTree(message.getPayload());
            if ("ping".equals(node.path("type").asText())) {
                sendJson(session, Map.of("type", "pong"));
            }
        } catch (Exception e) {
            log.warn("WS message parse failed", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            ONLINE_SESSIONS.remove(userId, session);
        }
    }

    public void sendToUser(Long userId, String json) {
        WebSocketSession session = ONLINE_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            sendRaw(session, json);
        }
    }

    public void sendNotice(Long userId, String title, String content) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "notice",
                    "title", title,
                    "content", content,
                    "time", System.currentTimeMillis()));
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
            payload.put("type", "groupChat");
            payload.put("time", System.currentTimeMillis());
            sendToUser(userId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("sendGroupChatPayload failed", e);
        }
    }

    public boolean isOnline(Long userId) {
        WebSocketSession session = ONLINE_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    private void broadcast(String json) {
        ONLINE_SESSIONS.values().forEach(s -> sendRaw(s, json));
    }

    private void sendJson(WebSocketSession session, Map<String, Object> map) {
        try {
            sendRaw(session, objectMapper.writeValueAsString(map));
        } catch (Exception ignored) {
        }
    }

    private void sendRaw(WebSocketSession session, String json) {
        if (session != null && session.isOpen()) {
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

    private void closeQuietly(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }
    }
}
