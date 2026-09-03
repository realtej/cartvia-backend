package com.cartvia.cartvia_backend.shoppinglist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShoppingListItemRequest {

    @NotNull
    private Boolean purchased;
}
