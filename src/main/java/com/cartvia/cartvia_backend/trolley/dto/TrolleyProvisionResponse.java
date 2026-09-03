package com.cartvia.cartvia_backend.trolley.dto;

import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;

import java.util.UUID;

public record TrolleyProvisionResponse(
        UUID id,
        String trolleyCode,
        UUID storeId,
        TrolleyStatus status,
        String qrPayload,
        String deviceToken) {
}
