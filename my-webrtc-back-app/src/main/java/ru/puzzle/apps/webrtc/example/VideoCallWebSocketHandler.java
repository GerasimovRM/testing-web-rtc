package ru.puzzle.apps.webrtc.example;

import org.json.JSONObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class VideoCallWebSocketHandler extends TextWebSocketHandler {
    private final Map<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject jsonMessage = new JSONObject(message.getPayload());
        String type = jsonMessage.getString("type");

        switch (type) {
            case "join-from-front":
                handleJoin(session, jsonMessage);
                break;

            case "offer-from-front":
            case "answer-from-front":
            case "candidate-from-front":
                handleOfferAnswerCandidate(session, message);
                break;
        }
    }

    private void handleJoin(WebSocketSession session, JSONObject jsonMessage) throws Exception {
        String userId = jsonMessage.getString("userId");
        sessionUserMap.put(session, userId);
        AtomicInteger count = new AtomicInteger();
        sessionUserMap.forEach((webSocketSession, user) -> {
            System.out.println(count.getAndIncrement() + " Session: " + webSocketSession + " User: " + user);
        });
        broadcastMessage(new JSONObject().put("type", "new-user-from-back").put("userId", userId));
    }

    private void handleOfferAnswerCandidate(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject jsonMessage = new JSONObject(message.getPayload());
        System.out.println("================");
        System.out.println("Type: " + jsonMessage.getString("type"));
        System.out.println("Session: " + session);
        System.out.println("UserId: " + sessionUserMap.get(session));

        String targetUserId = jsonMessage.getString("targetUserId");

        WebSocketSession targetSession = findSessionByUserId(targetUserId);
        System.out.println("UserSendId: " + targetUserId);
        System.out.println("TargetSession: " + targetSession);
        System.out.println("================");

        jsonMessage.put("targetUserId", sessionUserMap.get(session));
        jsonMessage.put("type", ((String) jsonMessage.get("type"))
                .replace("-from-front", "-from-back"));


        if (targetSession != null) {
            targetSession.sendMessage(new TextMessage(jsonMessage.toString()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = sessionUserMap.get(session);
        System.out.println("User disconnected: " + userId);
        sessionUserMap.remove(session);
        broadcastMessage(new JSONObject().put("type", "user-left-from-back").put("userId", userId));
    }

    private WebSocketSession findSessionByUserId(String userId) {
        for (Map.Entry<WebSocketSession, String> entry : sessionUserMap.entrySet()) {
            if (entry.getValue().equals(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void broadcastMessage(JSONObject message) throws Exception {
        for (WebSocketSession session : sessionUserMap.keySet()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message.toString()));
            }
        }
    }
}

