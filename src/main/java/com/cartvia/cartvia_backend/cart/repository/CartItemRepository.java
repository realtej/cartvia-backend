package com.cartvia.cartvia_backend.cart.repository;

import com.cartvia.cartvia_backend.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCart_Id(UUID cartId);

    Optional<CartItem> findByCart_IdAndProduct_Id(UUID cartId, UUID productId);

    void deleteByCart_IdAndProduct_Id(UUID cartId, UUID productId);
}
