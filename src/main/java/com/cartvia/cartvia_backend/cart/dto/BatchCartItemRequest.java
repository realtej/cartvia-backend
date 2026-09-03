package com.cartvia.cartvia_backend.cart.dto;

import com.cartvia.cartvia_backend.common.enums.CartSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BatchCartItemRequest {

    @NotBlank
    private String barcode;

    private CartSource source;

    @NotNull
    private LocalDateTime clientTs;
}
