package com.cartvia.cartvia_backend.receipt;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.entity.OrderItem;
import com.cartvia.cartvia_backend.order.repository.OrderRepository;
import com.cartvia.cartvia_backend.receipt.dto.ReceiptItemDto;
import com.cartvia.cartvia_backend.receipt.dto.ReceiptResponse;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * No dedicated Receipt entity or endpoint is defined anywhere in the specs
 * (checked CartRex-API-JSON-Bodies.md, CartRex-API-Error-Validation.md,
 * CartRex-Authorization-Spec.md — none mention "receipt"). Order + OrderItem
 * already carry every field a receipt needs (order code, line items,
 * subtotal/discount/tax/grand total, payment status via Order.status,
 * timestamp via createdAt), so a receipt is just a read-only reshaping of
 * that data — no new table, no write path.
 * <p>
 * Authorization mirrors OrderService.getOrder exactly (same resource, same
 * ownership rule): CUSTOMER may only view their own order's receipt, ADMIN
 * is unrestricted, STORE_STAFF/DEVICE/PUBLIC are denied.
 */
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final OrderRepository orderRepository;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(UUID orderId) {
        Role role = authenticationService.getAuthenticatedRole();

        if (role != Role.ADMIN) {
            authorizationService.requireRole(Role.CUSTOMER);
            authorizationService.requireCustomerOwnsOrder(orderId, authenticationService.getAuthenticatedUserId());
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));

        return toResponse(order);
    }

    private ReceiptResponse toResponse(Order order) {
        List<ReceiptItemDto> items = order.getItems().stream()
                .map(this::toItemDto)
                .toList();
        return new ReceiptResponse(
                order.getId(),
                order.getOrderCode(),
                order.getStore().getName(),
                order.getStatus(),
                items,
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getTaxTotal(),
                order.getGrandTotal(),
                order.getCreatedAt());
    }

    private ReceiptItemDto toItemDto(OrderItem item) {
        return new ReceiptItemDto(
                item.getProduct().getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
