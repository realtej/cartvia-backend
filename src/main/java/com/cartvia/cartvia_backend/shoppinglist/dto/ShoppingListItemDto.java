package com.cartvia.cartvia_backend.shoppinglist.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShoppingListItemDto(
        UUID itemId,
        UUID productId,
        String name,
        boolean purchased,
        LocalDateTime purchasedAt) {
}
