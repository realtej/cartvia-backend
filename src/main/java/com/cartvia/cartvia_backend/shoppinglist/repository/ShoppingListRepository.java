package com.cartvia.cartvia_backend.shoppinglist.repository;

import com.cartvia.cartvia_backend.shoppinglist.entity.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, UUID> {

    Optional<ShoppingList> findFirstByUser_IdOrderByCreatedAtDesc(UUID userId);
}
