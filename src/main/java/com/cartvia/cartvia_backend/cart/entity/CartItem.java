package com.cartvia.cartvia_backend.cart.entity;

import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import com.cartvia.cartvia_backend.common.enums.CartSource;
import com.cartvia.cartvia_backend.common.enums.VerificationStatus;
import com.cartvia.cartvia_backend.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_items_cart_product",
                columnNames = {"cart_id", "product_id"}),
        indexes = @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"))
@Getter
@Setter
@NoArgsConstructor
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CartSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "measured_weight_g", precision = 12, scale = 2)
    private BigDecimal measuredWeightG;

    private LocalDateTime clientTs;
}
