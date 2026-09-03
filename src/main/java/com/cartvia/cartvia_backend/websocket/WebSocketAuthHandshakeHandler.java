package com.cartvia.cartvia_backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class WebSocketAuthHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Object principal = attributes.get(WebSocketHandshakeInterceptor.PRINCIPAL_ATTR);
        if (principal instanceof Principal p) {
            return p;
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}
