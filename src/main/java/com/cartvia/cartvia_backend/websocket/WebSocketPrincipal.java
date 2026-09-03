package com.cartvia.cartvia_backend.websocket;

import java.security.Principal;
import java.util.UUID;

/**
 * Identifies the party behind a STOMP session after a successful handshake.
 * Either {@code userId} (customer JWT) or {@code trolleyId}/{@code trolleyCode}
 * (ESP32 device token) is populated, mirroring the two authentication paths
 * already used for REST (JwtAuthenticationFilter / DeviceTokenAuthenticationFilter).
 */
public record WebSocketPrincipal(UUID userId, UUID trolleyId, String trolleyCode) implements Principal {

    public static WebSocketPrincipal forUser(UUID userId) {
        return new WebSocketPrincipal(userId, null, null);
    }

    public static WebSocketPrincipal forDevice(UUID trolleyId, String trolleyCode) {
        return new WebSocketPrincipal(null, trolleyId, trolleyCode);
    }

    public boolean isDevice() {
        return trolleyId != null;
    }

    @Override
    public String getName() {
        return isDevice() ? "device:" + trolleyCode : "user:" + userId;
    }
}
