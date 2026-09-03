package com.cartvia.cartvia_backend.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InitiatePaymentRequest {

    @NotNull
    private UUID orderId;
}
