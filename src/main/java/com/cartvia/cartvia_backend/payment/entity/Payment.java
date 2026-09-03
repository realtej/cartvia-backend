package com.cartvia.cartvia_backend.payment.entity;

import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import com.cartvia.cartvia_backend.common.enums.PaymentStatus;
import com.cartvia.cartvia_backend.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order_id", columnList = "order_id"),
                @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(length = 100)
    private String upiTxnRef;

    @Column(length = 100)
    private String gatewayRef;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;
}
