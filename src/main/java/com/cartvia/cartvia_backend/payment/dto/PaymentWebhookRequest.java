package com.cartvia.cartvia_backend.payment.dto;

import com.cartvia.cartvia_backend.common.enums.WebhookStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PaymentWebhookRequest {

    @NotNull
    private UUID orderId;

    @NotNull
    private WebhookStatus status;

    @NotNull
    private String signature;

    private String upiTxnRef;
}
