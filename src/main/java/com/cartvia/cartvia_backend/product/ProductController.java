package com.cartvia.cartvia_backend.product;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.common.dto.PageResponse;
import com.cartvia.cartvia_backend.product.dto.ProductResponse;
import com.cartvia.cartvia_backend.product.dto.ProductUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> listProducts(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(productService.listProducts(storeId, search, category, page, size, false));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID storeId) {
        return ApiResponse.success(productService.getProduct(id, storeId));
    }

    @GetMapping("/barcode/{barcode}")
    public ApiResponse<ProductResponse> getByBarcode(
            @PathVariable String barcode,
            @RequestParam(required = false) UUID storeId) {
        return ApiResponse.success(productService.getByBarcode(barcode, storeId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductUpsertRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpsertRequest request) {
        return ApiResponse.success(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ApiResponse.successResponse();
    }
}
