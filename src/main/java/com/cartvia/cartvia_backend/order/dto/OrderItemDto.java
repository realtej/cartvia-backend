package com.cartvia.cartvia_backend.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID productId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
