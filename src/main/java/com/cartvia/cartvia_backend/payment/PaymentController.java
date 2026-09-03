package com.cartvia.cartvia_backend.payment;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.payment.dto.InitiatePaymentRequest;
import com.cartvia.cartvia_backend.payment.dto.InitiatePaymentResponse;
import com.cartvia.cartvia_backend.payment.dto.PaymentStatusResponse;
import com.cartvia.cartvia_backend.payment.dto.PaymentWebhookRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ApiResponse<InitiatePaymentResponse> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return ApiResponse.success(paymentService.initiatePayment(request.getOrderId()));
    }

    /**
     * Not RBAC-protected — permitted publicly in SecurityConfig. Authenticity
     * is established by verifying the gateway signature inside the service,
     * not by JWT/device/role (spec §11).
     */
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@Valid @RequestBody PaymentWebhookRequest request) {
        paymentService.handleWebhook(request);
        return ApiResponse.successResponse();
    }

    @GetMapping("/{orderId}/status")
    public ApiResponse<PaymentStatusResponse> getStatus(@PathVariable UUID orderId) {
        return ApiResponse.successResponse(paymentService.getPaymentStatus(orderId));
    }
}
