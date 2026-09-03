package com.cartvia.cartvia_backend.shoppinglist.repository;

import com.cartvia.cartvia_backend.shoppinglist.entity.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, UUID> {

    List<ShoppingListItem> findByShoppingList_Id(UUID listId);
}
