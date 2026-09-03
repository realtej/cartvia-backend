package com.cartvia.cartvia_backend.product;

import com.cartvia.cartvia_backend.common.dto.PageResponse;
import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.inventory.entity.Inventory;
import com.cartvia.cartvia_backend.inventory.repository.InventoryRepository;
import com.cartvia.cartvia_backend.product.dto.ProductResponse;
import com.cartvia.cartvia_backend.product.dto.ProductUpsertRequest;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.product.repository.ProductRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.store.StoreService;
import com.cartvia.cartvia_backend.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StoreService storeService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(UUID storeId, String search, String category,
                                                    int page, int size, boolean adminListing) {
        resolveListAuthorization(storeId);
        Pageable pageable = PageRequest.of(page, clampSize(size));
        boolean activeOnly = !adminListing && authenticationService.getAuthenticatedRole() == Role.CUSTOMER;
        Specification<Product> spec = ProductSpecifications.withFilters(storeId, search, category, activeOnly);
        Page<ProductResponse> result = productRepository.findAll(spec, pageable)
                .map(product -> toResponse(product, storeId));
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id, UUID storeId) {
        Product product = requireActiveProduct(id);
        return toResponse(product, storeId);
    }

    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode, UUID storeId) {
        Product product = productRepository.findByBarcode(barcode)
                .filter(Product::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Product not found for barcode " + barcode));
        return toResponse(product, storeId);
    }

    @Transactional
    public ProductResponse createProduct(ProductUpsertRequest request) {
        authorizationService.requireRole(Role.STORE_STAFF, Role.ADMIN);
        validateStoreAccessForWrite(request.getStoreId());
        ensureBarcodeUnique(request.getBarcode(), null);

        Product product = mapRequest(new Product(), request);
        product.setActive(true);
        product = productRepository.save(product);
        createInventoryIfNeeded(product);
        return toResponse(product, request.getStoreId());
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductUpsertRequest request) {
        authorizationService.requireRole(Role.STORE_STAFF, Role.ADMIN);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
        authorizationService.requireStoreStaffForProduct(id);
        validateStoreAccessForWrite(request.getStoreId());
        ensureBarcodeUnique(request.getBarcode(), id);

        mapRequest(product, request);
        product = productRepository.save(product);
        return toResponse(product, request.getStoreId());
    }

    @Transactional
    public void deleteProduct(UUID id) {
        authorizationService.requireRole(Role.STORE_STAFF, Role.ADMIN);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
        authorizationService.requireStoreStaffForProduct(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public Product requireActiveProduct(UUID id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void resolveListAuthorization(UUID storeId) {
        Role role = authenticationService.getAuthenticatedRole();
        if (role == Role.STORE_STAFF) {
            UUID staffStoreId = authenticationService.getAuthenticatedStoreId();
            if (storeId != null && !storeId.equals(staffStoreId)) {
                throw new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                        "You do not have permission to perform this operation");
            }
        }
    }

    private void validateStoreAccessForWrite(UUID storeId) {
        if (storeId == null) {
            if (authenticationService.getAuthenticatedRole() == Role.STORE_STAFF) {
                throw new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                        "You do not have permission to perform this operation");
            }
            return;
        }
        authorizationService.requireStoreStaffForStore(storeId);
    }

    private void ensureBarcodeUnique(String barcode, UUID excludeId) {
        boolean exists = excludeId == null
                ? productRepository.existsByBarcode(barcode)
                : productRepository.existsByBarcodeAndIdNot(barcode, excludeId);
        if (exists) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT,
                    "A product with this barcode already exists");
        }
    }

    private void createInventoryIfNeeded(Product product) {
        if (product.getStore() == null) {
            return;
        }
        Inventory inventory = new Inventory();
        inventory.setStore(product.getStore());
        inventory.setProduct(product);
        inventory.setStockQty(0);
        inventory.setLowStockThreshold(10);
        inventoryRepository.save(inventory);
    }

    private Product mapRequest(Product product, ProductUpsertRequest request) {
        product.setBarcode(request.getBarcode());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setDiscountPct(request.getDiscountPct() != null ? request.getDiscountPct() : BigDecimal.ZERO);
        product.setImageUrl(request.getImageUrl());
        product.setExpectedWeightG(request.getExpectedWeightG());
        product.setWeightTolerancePct(request.getWeightTolerancePct());
        product.setGstSlabPct(request.getGstSlabPct());
        if (request.getStoreId() != null) {
            Store store = storeService.requireStore(request.getStoreId());
            product.setStore(store);
        } else {
            product.setStore(null);
        }
        return product;
    }

    private ProductResponse toResponse(Product product, UUID storeId) {
        UUID inventoryStoreId = storeId != null
                ? storeId
                : (product.getStore() != null ? product.getStore().getId() : null);
        boolean inStock = resolveInStock(product, inventoryStoreId);
        return new ProductResponse(
                product.getId(),
                product.getBarcode(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getDiscountPct(),
                product.getExpectedWeightG(),
                product.getWeightTolerancePct(),
                product.getImageUrl(),
                inStock);
    }

    private boolean resolveInStock(Product product, UUID storeId) {
        if (storeId == null) {
            return true;
        }
        return inventoryRepository.findByStore_IdAndProduct_Id(storeId, product.getId())
                .map(inv -> inv.getStockQty() > 0)
                .orElse(false);
    }

    private int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, 100);
    }
}
