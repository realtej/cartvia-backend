package com.cartvia.cartvia_backend.receipt.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReceiptItemDto(
        UUID productId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
