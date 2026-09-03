package com.cartvia.cartvia_backend.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID storeId,
        int stockQty,
        int lowStockThreshold,
        LocalDateTime updatedAt) {
}
