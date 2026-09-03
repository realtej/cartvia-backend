package com.cartvia.cartvia_backend.websocket;

import com.cartvia.cartvia_backend.websocket.dto.WebSocketEvent;
import com.cartvia.cartvia_backend.websocket.dto.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Single place every module goes through to publish a real-time event, so
 * the topic naming convention (session/trolley scoped) stays consistent.
 * Publishing never throws into the caller's transaction — a broker hiccup
 * should not fail a checkout or cart mutation that already succeeded.
 */
@Service
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public <T> void toSession(UUID sessionId, WebSocketEventType type, T data) {
        send("/topic/sessions/" + sessionId, type, data);
    }

    public <T> void toTrolley(String trolleyCode, WebSocketEventType type, T data) {
        send("/topic/trolleys/" + trolleyCode, type, data);
    }

    private <T> void send(String destination, WebSocketEventType type, T data) {
        try {
            messagingTemplate.convertAndSend(destination, WebSocketEvent.of(type, data));
        } catch (RuntimeException ex) {
            log.warn("Failed to publish {} to {}: {}", type, destination, ex.getMessage());
        }
    }
}
