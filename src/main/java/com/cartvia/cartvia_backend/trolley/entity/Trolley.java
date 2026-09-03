package com.cartvia.cartvia_backend.trolley.entity;

import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;
import com.cartvia.cartvia_backend.store.entity.Store;
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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trolleys",
        uniqueConstraints = @UniqueConstraint(name = "uk_trolleys_code", columnNames = "trolley_code"),
        indexes = {
                @Index(name = "idx_trolleys_store_id", columnList = "store_id"),
                @Index(name = "idx_trolleys_device_token_hash", columnList = "device_token_hash")
        })
@Getter
@Setter
@NoArgsConstructor
public class Trolley extends BaseEntity {

    @Column(name = "trolley_code", nullable = false, length = 50)
    private String trolleyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(length = 17)
    private String esp32Mac;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrolleyStatus status = TrolleyStatus.INACTIVE;

    @Column(name = "device_token_hash", length = 128)
    private String deviceTokenHash;

    private LocalDateTime lastSeenAt;

    private Integer batteryPct;

    private Integer rssi;
}
