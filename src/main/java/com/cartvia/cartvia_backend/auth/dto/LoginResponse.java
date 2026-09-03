package com.cartvia.cartvia_backend.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserSummaryDto user) {
}
