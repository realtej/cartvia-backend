package com.cartvia.cartvia_backend.payment.entity;

import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_events_external_id", columnNames = "external_id"))
@Getter
@Setter
@NoArgsConstructor
public class WebhookEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "external_id", nullable = false, length = 200)
    private String externalId;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}
