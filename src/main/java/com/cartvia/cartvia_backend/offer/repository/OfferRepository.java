package com.cartvia.cartvia_backend.offer.repository;

import com.cartvia.cartvia_backend.offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    List<Offer> findByStore_IdAndActiveTrue(UUID storeId);

    List<Offer> findByStore_IdAndProduct_IdAndActiveTrue(UUID storeId, UUID productId);

    List<Offer> findByStore_Id(UUID storeId);

    List<Offer> findByProduct_Id(UUID productId);

    List<Offer> findByStore_IdAndProduct_Id(UUID storeId, UUID productId);
}
