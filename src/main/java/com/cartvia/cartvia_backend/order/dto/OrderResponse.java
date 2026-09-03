package com.cartvia.cartvia_backend.order.dto;

import com.cartvia.cartvia_backend.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String orderCode,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal grandTotal,
        List<OrderItemDto> items,
        LocalDateTime createdAt) {
}
