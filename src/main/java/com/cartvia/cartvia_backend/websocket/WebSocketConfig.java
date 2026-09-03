package com.cartvia.cartvia_backend.websocket;

import com.cartvia.cartvia_backend.config.CartviaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time layer for cart/session/trolley events (see WebSocketEventType).
 * Endpoint path and allowed origins come from cartvia.websocket.* rather
 * than being hardcoded (CartviaProperties.Websocket).
 * <p>
 * Topics are session/trolley scoped:
 *   /topic/sessions/{sessionId} — CART_UPDATED, WEIGHT_UPDATED,
 *     WEIGHT_VALIDATION_RESULT, PRODUCT_SCANNED, PRODUCT_ADDED,
 *     PRODUCT_REMOVED, CHECKOUT_STARTED, PAYMENT_COMPLETED, SESSION_UPDATED
 *   /topic/trolleys/{trolleyCode} — TROLLEY_CONNECTED, TROLLEY_DISCONNECTED
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CartviaProperties cartviaProperties;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final WebSocketAuthHandshakeHandler handshakeHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = cartviaProperties.getWebsocket().getAllowedOrigins().split(",");

        // Plain STOMP-over-WebSocket (no SockJS fallback) — both the Android
        // app and the ESP32 firmware speak native WebSocket, so the extra
        // SockJS transport negotiation isn't needed here.
        registry.addEndpoint(cartviaProperties.getWebsocket().getEndpoint())
                .setAllowedOriginPatterns(allowedOrigins)
                .addInterceptors(handshakeInterceptor)
                .setHandshakeHandler(handshakeHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
