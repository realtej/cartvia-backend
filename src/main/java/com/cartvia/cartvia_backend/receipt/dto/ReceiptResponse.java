package com.cartvia.cartvia_backend.receipt.dto;

import com.cartvia.cartvia_backend.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReceiptResponse(
        UUID orderId,
        String orderCode,
        String storeName,
        OrderStatus paymentStatus,
        List<ReceiptItemDto> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal grandTotal,
        LocalDateTime issuedAt) {
}
