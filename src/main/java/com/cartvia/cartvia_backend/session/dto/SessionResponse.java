package com.cartvia.cartvia_backend.session.dto;

import com.cartvia.cartvia_backend.common.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID sessionId,
        String sessionCode,
        UUID userId,
        String trolleyCode,
        UUID storeId,
        SessionStatus status,
        UUID cartId,
        BigDecimal cartTotal,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {
}
