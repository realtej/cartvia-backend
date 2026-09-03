package com.cartvia.cartvia_backend.offer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID storeId,
        UUID productId,
        String title,
        BigDecimal discountPct,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        boolean active) {
}
