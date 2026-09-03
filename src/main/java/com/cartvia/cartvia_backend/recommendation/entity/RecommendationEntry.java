package com.cartvia.cartvia_backend.recommendation.entity;

import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import com.cartvia.cartvia_backend.common.enums.RecommendationType;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.user.entity.User;
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

@Entity
@Table(
        name = "recommendation_entries",
        indexes = {
                @Index(name = "idx_recommendations_user_id", columnList = "user_id"),
                @Index(name = "idx_recommendations_product_id", columnList = "product_id"),
                @Index(name = "idx_recommendations_category", columnList = "category")
        })
@Getter
@Setter
@NoArgsConstructor
public class RecommendationEntry extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecommendationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_product_id")
    private Product relatedProduct;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private double score = 0.0;
}
