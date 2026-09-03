package com.cartvia.cartvia_backend.websocket.dto;

import java.time.LocalDateTime;

public record WebSocketEvent<T>(
        WebSocketEventType type,
        T data,
        LocalDateTime timestamp) {

    public static <T> WebSocketEvent<T> of(WebSocketEventType type, T data) {
        return new WebSocketEvent<>(type, data, LocalDateTime.now());
    }
}
