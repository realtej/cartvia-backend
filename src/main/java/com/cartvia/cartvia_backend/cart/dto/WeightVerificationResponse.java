package com.cartvia.cartvia_backend.cart.dto;

import com.cartvia.cartvia_backend.common.enums.VerificationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record WeightVerificationResponse(
        UUID productId,
        VerificationStatus verificationStatus,
        BigDecimal measuredWeightG,
        BigDecimal expectedWeightG) {
}
