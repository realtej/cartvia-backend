package com.cartvia.cartvia_backend.cart.dto;

import com.cartvia.cartvia_backend.common.enums.CartSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AddCartItemRequest {

    @NotBlank
    private String barcode;

    private CartSource source;
}
