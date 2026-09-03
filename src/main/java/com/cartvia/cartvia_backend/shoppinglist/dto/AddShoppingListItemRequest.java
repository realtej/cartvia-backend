package com.cartvia.cartvia_backend.shoppinglist.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AddShoppingListItemRequest {

    private UUID productId;

    private String customName;
}
