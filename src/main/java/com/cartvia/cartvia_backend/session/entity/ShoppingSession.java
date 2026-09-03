package com.cartvia.cartvia_backend.session.entity;

import com.cartvia.cartvia_backend.cart.entity.Cart;
import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import com.cartvia.cartvia_backend.common.enums.SessionStatus;
import com.cartvia.cartvia_backend.store.entity.Store;
import com.cartvia.cartvia_backend.trolley.entity.Trolley;
import com.cartvia.cartvia_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shopping_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_sessions_code", columnNames = "session_code"),
        indexes = {
                @Index(name = "idx_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_sessions_trolley_status", columnList = "trolley_id, status"),
                @Index(name = "idx_sessions_store_id", columnList = "store_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class ShoppingSession extends BaseEntity {

    @Column(name = "session_code", nullable = false, length = 50)
    private String sessionCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trolley_id", nullable = false)
    private Trolley trolley;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @OneToOne(mappedBy = "session", fetch = FetchType.LAZY)
    private Cart cart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
