package com.cartvia.cartvia_backend.security;

import com.cartvia.cartvia_backend.config.CartviaProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DeviceTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Pattern HEARTBEAT_PATH = Pattern.compile("^/api/trolleys/([^/]+)/heartbeat$");
    private static final Pattern CART_ITEMS_PATH = Pattern.compile("^/api/carts/[^/]+/items(?:/batch|/[^/]+/verify-weight)?$");

    private final CartviaProperties properties;
    private final DeviceAuthenticationService deviceAuthenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isDevicePath(request) || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String deviceToken = request.getHeader(properties.getDevice().getTokenHeader());
        String path = request.getRequestURI();

        Matcher heartbeatMatcher = HEARTBEAT_PATH.matcher(path);
        if (heartbeatMatcher.matches() && deviceToken != null) {
            String trolleyCode = heartbeatMatcher.group(1);
            authenticateDevice(deviceToken, trolleyCode, request);
        } else if (CART_ITEMS_PATH.matcher(path).matches() && deviceToken != null) {
            deviceAuthenticationService.authenticateByToken(deviceToken)
                    .ifPresent(principal -> setAuthentication(principal, request));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateDevice(String deviceToken, String trolleyCode, HttpServletRequest request) {
        deviceAuthenticationService.authenticate(deviceToken, trolleyCode)
                .ifPresent(principal -> setAuthentication(principal, request));
    }

    private void setAuthentication(DevicePrincipal principal, HttpServletRequest request) {
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isDevicePath(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return HEARTBEAT_PATH.matcher(path).matches() || CART_ITEMS_PATH.matcher(path).matches();
    }
}
