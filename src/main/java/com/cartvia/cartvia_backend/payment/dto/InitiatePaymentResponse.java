package com.cartvia.cartvia_backend.payment.dto;

import com.cartvia.cartvia_backend.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentResponse(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        String upiDeepLink,
        String qrPayload) {
}
