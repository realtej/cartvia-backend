package com.cartvia.cartvia_backend.offer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class OfferRequest {

    @NotNull
    private UUID storeId;

    private UUID productId;

    @NotBlank
    private String title;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal discountPct;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private boolean active = true;
}
