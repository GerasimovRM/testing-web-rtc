package ru.puzzle.apps.webrtc.example.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import ru.puzzle.apps.webrtc.example.components.VideoCallWebSocketHandler;

import java.util.Map;

@Configuration
@EnableWebFlux
@RequiredArgsConstructor
public class WebSocketConfig {
    private final VideoCallWebSocketHandler videoCallWebSocketHandler;

    @Bean
    public HandlerMapping handlerMapping() {
        Map<String, VideoCallWebSocketHandler> handlerMap = Map.of(
                "/video-call", videoCallWebSocketHandler
        );
        return new SimpleUrlHandlerMapping(handlerMap, 1);
    }
}