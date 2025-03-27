package ru.puzzle.apps.webrtc.example.components;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class VideoCallWebSocketHandler implements WebSocketHandler {
    private final Map<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public @NonNull Mono<Void> handle(@NonNull WebSocketSession session) {
        session.getHandshakeInfo().getCookies().forEach((name, cookies) -> cookies.forEach(cookie -> log.info("Cookie: {} = {}", name, cookie.getValue())));

        return session.receive()
                .doOnTerminate(() -> {
                    log.debug("WebSocket session terminated: {}", session.getId());
                    String leftedUserId = sessionUserMap.get(session);
                    sessionUserMap.remove(session);
                    broadcastMessage(new JSONObject().put("type", "user-left").put("userId", leftedUserId));
                })
                .map(webSocketMessage -> {
                    String payload = webSocketMessage.getPayloadAsText();
                    JSONObject jsonMessage = new JSONObject(payload);
                    String type = jsonMessage.getString("type");

                    switch (type) {
                        case "join":
                            handleJoin(session, jsonMessage);
                            break;
                        case "answer", "offer", "candidate":
                            handleOfferAnswerCandidate(session, jsonMessage);
                            break;
                        default:
                            log.warn("Unknown websocket message type: {}", type);
                            break;
                    }
                    return webSocketMessage;
                })
                .then();
    }

    private void handleJoin(WebSocketSession session, JSONObject jsonMessage) {
        String userId = jsonMessage.getString("userId");
        sessionUserMap.put(session, userId);
        AtomicInteger count = new AtomicInteger();
        sessionUserMap.forEach((webSocketSession, user) -> log.debug("{} Session: {} User: {}", count.getAndIncrement(), webSocketSession, user));

        broadcastMessage(new JSONObject().put("type", "new-user").put("userId", userId));
    }

    private void handleOfferAnswerCandidate(WebSocketSession session, JSONObject jsonMessage) {
        String targetUserId = jsonMessage.getString("targetUserId");
        WebSocketSession targetSession = findSessionByUserId(targetUserId);
        jsonMessage.put("targetUserId", sessionUserMap.get(session));
        jsonMessage.put("type", jsonMessage.get("type"));

        if (targetSession != null && targetSession.isOpen()) {
            targetSession.send(Flux.just(session.textMessage(jsonMessage.toString())))
                    .doOnError(error -> log.error("Error sending message: {}", error.getMessage()))
                    .subscribe();
        }
    }

    private WebSocketSession findSessionByUserId(String userId) {
        for (Map.Entry<WebSocketSession, String> entry : sessionUserMap.entrySet()) {
            if (entry.getValue().equals(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void broadcastMessage(JSONObject message) {
        sessionUserMap.keySet().forEach(session -> {
            if (session.isOpen()) {
                session.send(Flux.just(session.textMessage(message.toString())))
                        .doOnError(error -> log.error("Error sending message: {}", error.getMessage()))
                        .subscribe();
            }
        });
    }
}
