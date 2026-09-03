package com.cartvia.cartvia_backend.cart.dto;

import com.cartvia.cartvia_backend.common.enums.VerificationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDto(
        UUID productId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        VerificationStatus verificationStatus) {

    public CartItemDto(UUID productId, String name, int quantity, BigDecimal unitPrice) {
        this(productId, name, quantity, unitPrice, null);
    }
}
