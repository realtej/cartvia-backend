package com.cartvia.cartvia_backend.order.repository;

import com.cartvia.cartvia_backend.common.enums.OrderStatus;
import com.cartvia.cartvia_backend.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderCode(String orderCode);

    Page<Order> findByStore_IdAndStatus(UUID storeId, OrderStatus status, Pageable pageable);

    Page<Order> findByStore_Id(UUID storeId, Pageable pageable);
}
