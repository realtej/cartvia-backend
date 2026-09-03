package com.cartvia.cartvia_backend.offer.entity;

import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.store.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "offers",
        indexes = {
                @Index(name = "idx_offers_store_id", columnList = "store_id"),
                @Index(name = "idx_offers_product_id", columnList = "product_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Offer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    @Column(nullable = false)
    private boolean active = true;
}
