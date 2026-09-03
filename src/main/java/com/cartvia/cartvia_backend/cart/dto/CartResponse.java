package com.cartvia.cartvia_backend.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        UUID sessionId,
        BigDecimal totalAmount,
        List<CartItemDto> items) {
}
