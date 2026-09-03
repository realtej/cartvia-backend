package com.cartvia.cartvia_backend.session.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record EndSessionResponse(
        SessionResponse session,
        UUID orderId,
        String orderCode,
        BigDecimal grandTotal) {
}
