package com.cartvia.cartvia_backend.payment.repository;

import com.cartvia.cartvia_backend.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    boolean existsByExternalId(String externalId);
}
