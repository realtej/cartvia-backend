package com.cartvia.cartvia_backend.inventory.repository;

import com.cartvia.cartvia_backend.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    List<Inventory> findByStore_Id(UUID storeId);

    List<Inventory> findByStore_IdAndStockQtyLessThanEqual(UUID storeId, int threshold);

    Optional<Inventory> findByStore_IdAndProduct_Id(UUID storeId, UUID productId);
}
