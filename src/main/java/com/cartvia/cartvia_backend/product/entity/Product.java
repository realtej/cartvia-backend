package com.cartvia.cartvia_backend.product.entity;

import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import com.cartvia.cartvia_backend.store.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
        name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_barcode", columnNames = "barcode"),
        indexes = {
                @Index(name = "idx_products_store_id", columnList = "store_id"),
                @Index(name = "idx_products_category", columnList = "category"),
                @Index(name = "idx_products_name", columnList = "name")
        })
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String barcode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "expected_weight_g", precision = 12, scale = 2)
    private BigDecimal expectedWeightG;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightTolerancePct;

    @Column(precision = 5, scale = 2)
    private BigDecimal gstSlabPct;

    @Column(length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false)
    private boolean active = true;
}
