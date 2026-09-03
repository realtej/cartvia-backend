package com.cartvia.cartvia_backend.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchAddRequest {

    @NotEmpty
    @Valid
    private List<BatchCartItemRequest> items;
}
