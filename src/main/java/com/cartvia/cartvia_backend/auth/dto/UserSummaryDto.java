package com.cartvia.cartvia_backend.auth.dto;

import com.cartvia.cartvia_backend.common.enums.Role;

import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String username,
        String name,
        String email,
        String phone,
        Role role) {
}
