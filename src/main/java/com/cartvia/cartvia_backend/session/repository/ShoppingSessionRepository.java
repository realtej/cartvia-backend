package com.cartvia.cartvia_backend.session.repository;

import com.cartvia.cartvia_backend.common.enums.SessionStatus;
import com.cartvia.cartvia_backend.session.entity.ShoppingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingSessionRepository extends JpaRepository<ShoppingSession, UUID> {

    Optional<ShoppingSession> findBySessionCode(String sessionCode);

    Optional<ShoppingSession> findByTrolley_IdAndStatus(UUID trolleyId, SessionStatus status);

    List<ShoppingSession> findByUser_Id(UUID userId);
}
