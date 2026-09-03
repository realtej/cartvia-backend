package com.cartvia.cartvia_backend.websocket.dto;

import java.util.UUID;

public record ProductScannedDto(UUID productId, String name, String barcode) {
}
