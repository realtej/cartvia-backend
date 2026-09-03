package com.cartvia.cartvia_backend.websocket;

import com.cartvia.cartvia_backend.security.DeviceAuthenticationService;
import com.cartvia.cartvia_backend.security.DevicePrincipal;
import com.cartvia.cartvia_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * The customer Android app authenticates the WS handshake with
 * {@code ?token=<JWT access token>} (same token used for the Authorization
 * header on REST calls). The ESP32 trolley authenticates with
 * {@code ?deviceToken=<device token>&trolleyCode=<code>} (same credentials
 * used for the device-token REST paths). Exactly one of the two must be
 * present and valid, or the handshake is rejected with 401 before the
 * upgrade completes.
 */
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_ATTR = "cartviaPrincipal";

    private final JwtService jwtService;
    private final DeviceAuthenticationService deviceAuthenticationService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        var params = UriComponentsBuilder.fromUri(servletRequest.getURI()).build().getQueryParams();
        String token = params.getFirst("token");
        String deviceToken = params.getFirst("deviceToken");
        String trolleyCode = params.getFirst("trolleyCode");

        if (token != null && !token.isBlank()) {
            try {
                UUID userId = jwtService.extractUserId(token);
                attributes.put(PRINCIPAL_ATTR, WebSocketPrincipal.forUser(userId));
                return true;
            } catch (RuntimeException ex) {
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }
        }

        if (deviceToken != null && trolleyCode != null) {
            var principalOpt = deviceAuthenticationService.authenticate(deviceToken, trolleyCode);
            if (principalOpt.isPresent()) {
                DevicePrincipal device = principalOpt.get();
                attributes.put(PRINCIPAL_ATTR, WebSocketPrincipal.forDevice(device.getTrolleyId(), device.getTrolleyCode()));
                return true;
            }
        }

        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
