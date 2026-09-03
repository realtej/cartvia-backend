package com.cartvia.cartvia_backend.product.repository;

import com.cartvia.cartvia_backend.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, UUID id);
}
