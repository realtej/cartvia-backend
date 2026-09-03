package com.cartvia.cartvia_backend.store.repository;

import com.cartvia.cartvia_backend.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
}
