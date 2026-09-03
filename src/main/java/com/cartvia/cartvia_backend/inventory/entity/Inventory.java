package com.cartvia.cartvia_backend.inventory.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_store_product",
                columnNames = {"store_id", "product_id"}),
        indexes = @Index(name = "idx_inventory_store_id", columnList = "store_id"))
@Getter
@Setter
@NoArgsConstructor
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int stockQty = 0;

    @Column(nullable = false)
    private int lowStockThreshold = 10;
}
