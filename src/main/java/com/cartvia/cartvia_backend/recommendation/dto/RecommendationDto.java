package com.cartvia.cartvia_backend.recommendation.dto;

import com.cartvia.cartvia_backend.common.enums.RecommendationType;

import java.util.UUID;

public record RecommendationDto(
        UUID productId,
        String name,
        RecommendationType reason) {
}
