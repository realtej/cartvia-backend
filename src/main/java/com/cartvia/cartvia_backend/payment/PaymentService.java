package com.cartvia.cartvia_backend.payment;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.OrderStatus;
import com.cartvia.cartvia_backend.common.enums.PaymentStatus;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.common.enums.WebhookStatus;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.repository.OrderRepository;
import com.cartvia.cartvia_backend.payment.dto.InitiatePaymentResponse;
import com.cartvia.cartvia_backend.payment.dto.PaymentStatusResponse;
import com.cartvia.cartvia_backend.payment.dto.PaymentWebhookRequest;
import com.cartvia.cartvia_backend.payment.entity.Payment;
import com.cartvia.cartvia_backend.payment.entity.WebhookEvent;
import com.cartvia.cartvia_backend.payment.repository.PaymentRepository;
import com.cartvia.cartvia_backend.payment.repository.WebhookEventRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.security.TokenHashService;
import com.cartvia.cartvia_backend.websocket.WebSocketEventPublisher;
import com.cartvia.cartvia_backend.websocket.dto.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final PaymentSignatureService paymentSignatureService;
    private final TokenHashService tokenHashService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final WebSocketEventPublisher webSocketEventPublisher;

    /**
     * Customer → Own Order → Initiate Payment (spec §11.2). Only CUSTOMER may
     * initiate, and only for their own order (§7.2, role matrix: no ADMIN
     * bypass here, unlike order/status lookup).
     */
    @Transactional
    public InitiatePaymentResponse initiatePayment(UUID orderId) {
        authorizationService.requireRole(Role.CUSTOMER);
        UUID authenticatedUserId = authenticationService.getAuthenticatedUserId();
        authorizationService.requireCustomerOwnsOrder(orderId, authenticatedUserId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT,
                    "Order is not in a payable state");
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT,
                    "Order is not in a payable state");
        }

        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
        }
        payment.setAmount(order.getGrandTotal());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setIdempotencyKey(tokenHashService.generateOpaqueToken());
        payment = paymentRepository.save(payment);

        String upiDeepLink = paymentGatewayService.buildUpiDeepLink(order);
        String qrPayload = paymentGatewayService.buildQrPayload(order);

        return new InitiatePaymentResponse(
                payment.getId(),
                order.getId(),
                payment.getAmount(),
                payment.getStatus(),
                upiDeepLink,
                qrPayload);
    }

    /**
     * Payment webhook processing (spec §11). Authentication here is signature
     * verification, not RBAC — this endpoint is reached unauthenticated
     * (permitAll in SecurityConfig) and must authenticate the *event* itself.
     * <p>
     * Idempotent by design: the signature uniquely identifies a gateway
     * event, so a replayed webhook with the same signature is a silent no-op
     * rather than a duplicate state transition.
     */
    @Transactional
    public void handleWebhook(PaymentWebhookRequest request) {
        boolean validSignature = paymentSignatureService.isValid(
                request.getOrderId(), request.getStatus().name(), request.getUpiTxnRef(), request.getSignature());
        if (!validSignature) {
            throw new ApiException(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        if (webhookEventRepository.existsByExternalId(request.getSignature())) {
            // Already processed this exact gateway event — idempotent no-op.
            return;
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));

        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "No payment initiated for this order"));

        // Payment already resolved to a terminal state — do not re-apply.
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            recordWebhookEvent(request.getSignature(), payment);
            return;
        }

        if (request.getStatus() == WebhookStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            if (request.getUpiTxnRef() != null) {
                payment.setUpiTxnRef(request.getUpiTxnRef());
            }
            order.setStatus(OrderStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.FAILED);
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
        recordWebhookEvent(request.getSignature(), payment);

        if (request.getStatus() == WebhookStatus.SUCCESS && order.getSession() != null) {
            webSocketEventPublisher.toSession(order.getSession().getId(), WebSocketEventType.PAYMENT_COMPLETED,
                    new PaymentStatusResponse(order.getId(), payment.getId(), payment.getStatus(), payment.getAmount()));
        }
    }

    /**
     * CUSTOMER (ownership check) or ADMIN (unrestricted) per the role matrix
     * for GET /api/payments/{orderId}/status. STORE_STAFF/DEVICE denied.
     */
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID orderId) {
        Role role = authenticationService.getAuthenticatedRole();
        if (role != Role.ADMIN) {
            authorizationService.requireRole(Role.CUSTOMER);
            authorizationService.requireCustomerOwnsOrder(orderId, authenticationService.getAuthenticatedUserId());
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "No payment initiated for this order"));

        return new PaymentStatusResponse(orderId, payment.getId(), payment.getStatus(), payment.getAmount());
    }

    private void recordWebhookEvent(String externalId, Payment payment) {
        WebhookEvent event = new WebhookEvent();
        event.setPayment(payment);
        event.setExternalId(externalId);
        event.setProcessedAt(LocalDateTime.now());
        webhookEventRepository.save(event);
    }
}
