package com.cartvia.cartvia_backend.cart.repository;

import com.cartvia.cartvia_backend.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findBySession_Id(UUID sessionId);
}
