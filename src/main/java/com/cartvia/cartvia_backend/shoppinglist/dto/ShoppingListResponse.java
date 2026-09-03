package com.cartvia.cartvia_backend.shoppinglist.dto;

import java.util.List;
import java.util.UUID;

public record ShoppingListResponse(
        UUID listId,
        String name,
        List<ShoppingListItemDto> items) {
}
