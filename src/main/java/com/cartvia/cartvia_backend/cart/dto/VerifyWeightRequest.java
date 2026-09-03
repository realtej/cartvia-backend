package com.cartvia.cartvia_backend.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class VerifyWeightRequest {

    @NotNull
    @PositiveOrZero
    private BigDecimal measuredWeightG;
}
