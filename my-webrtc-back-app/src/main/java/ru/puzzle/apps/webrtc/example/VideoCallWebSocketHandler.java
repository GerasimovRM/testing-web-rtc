package ru.puzzle.apps.webrtc.example;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class VideoCallWebSocketHandler extends TextWebSocketHandler {
    private final Map<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws IOException {
        JSONObject jsonMessage = new JSONObject(message.getPayload());
        String type = jsonMessage.getString("type");
        switch (type) {
            case "join":
                handleJoin(session, jsonMessage);
                break;
            case "answer", "offer", "candidate":
                handleOfferAnswerCandidate(session, message);
                break;
            default:
                log.warn("Unknown websocket message type: {}", type);
                break;
        }
    }

    private void handleJoin(WebSocketSession session, JSONObject jsonMessage) throws IOException {
        String userId = jsonMessage.getString("userId");
        sessionUserMap.put(session, userId);
        AtomicInteger count = new AtomicInteger();
        sessionUserMap.forEach((webSocketSession, user) -> log.debug("{} Session: {} User: {}", count.getAndIncrement(), webSocketSession, user));
        broadcastMessage(new JSONObject().put("type", "new-user").put("userId", userId));
    }

    private void handleOfferAnswerCandidate(WebSocketSession session, TextMessage message) throws IOException {
        JSONObject jsonMessage = new JSONObject(message.getPayload());
        log.debug("================");
        log.debug("Type: {}", jsonMessage.getString("type"));
        log.debug("Session: {}", session);
        log.debug("UserId: {}", sessionUserMap.get(session));

        String targetUserId = jsonMessage.getString("targetUserId");

        WebSocketSession targetSession = findSessionByUserId(targetUserId);
        log.debug("UserSendId: {}", targetUserId);
        log.debug("TargetSession: {}", targetSession);
        log.debug("================");

        jsonMessage.put("targetUserId", sessionUserMap.get(session));
        jsonMessage.put("type", jsonMessage.get("type"));


        if (targetSession != null) {
            targetSession.sendMessage(new TextMessage(jsonMessage.toString()));
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws IOException {
        String userId = sessionUserMap.get(session);
        log.debug("User disconnected: {}", userId);
        sessionUserMap.remove(session);
        broadcastMessage(new JSONObject()
                .put("type", "user-left")
                .put("userId", userId)
        );
    }

    private WebSocketSession findSessionByUserId(String userId) {
        for (Map.Entry<WebSocketSession, String> entry : sessionUserMap.entrySet()) {
            if (entry.getValue().equals(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void broadcastMessage(JSONObject message) throws IOException {
        for (WebSocketSession session : sessionUserMap.keySet()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message.toString()));
            }
        }
    }
}

