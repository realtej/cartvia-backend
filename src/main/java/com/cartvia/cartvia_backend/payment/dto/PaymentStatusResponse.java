package com.cartvia.cartvia_backend.payment.dto;

import com.cartvia.cartvia_backend.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentStatusResponse(
        UUID orderId,
        UUID paymentId,
        PaymentStatus status,
        BigDecimal amount) {
}
