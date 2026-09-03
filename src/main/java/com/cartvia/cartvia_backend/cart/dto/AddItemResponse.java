package com.cartvia.cartvia_backend.cart.dto;

import com.cartvia.cartvia_backend.recommendation.dto.RecommendationDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddItemResponse(
        UUID cartId,
        CartItemDto item,
        BigDecimal cartTotal,
        List<RecommendationDto> recommendations) {
}
