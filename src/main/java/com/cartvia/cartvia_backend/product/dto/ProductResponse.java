package com.cartvia.cartvia_backend.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID productId,
        String barcode,
        String name,
        String description,
        String category,
        BigDecimal price,
        BigDecimal discountPct,
        BigDecimal expectedWeightG,
        BigDecimal weightTolerancePct,
        String imageUrl,
        boolean inStock) {
}
