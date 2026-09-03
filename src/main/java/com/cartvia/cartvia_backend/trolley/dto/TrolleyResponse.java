package com.cartvia.cartvia_backend.trolley.dto;

import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TrolleyResponse(
        UUID id,
        String trolleyCode,
        UUID storeId,
        TrolleyStatus status,
        String qrPayload,
        LocalDateTime lastSeenAt) {
}
