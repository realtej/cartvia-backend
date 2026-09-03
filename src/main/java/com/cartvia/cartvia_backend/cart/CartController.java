package com.cartvia.cartvia_backend.cart;

import com.cartvia.cartvia_backend.cart.dto.AddCartItemRequest;
import com.cartvia.cartvia_backend.cart.dto.AddItemResponse;
import com.cartvia.cartvia_backend.cart.dto.BatchAddRequest;
import com.cartvia.cartvia_backend.cart.dto.CartResponse;
import com.cartvia.cartvia_backend.cart.dto.UpdateCartItemRequest;
import com.cartvia.cartvia_backend.cart.dto.VerifyWeightRequest;
import com.cartvia.cartvia_backend.cart.dto.WeightVerificationResponse;
import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/{sessionId}/items")
    public ApiResponse<AddItemResponse> addItem(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addItem(sessionId, request));
    }

    @PostMapping("/{sessionId}/items/batch")
    public ApiResponse<CartResponse> batchAddItems(
            @PathVariable UUID sessionId,
            @Valid @RequestBody BatchAddRequest request) {
        return ApiResponse.success(cartService.batchAddItems(sessionId, request));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<CartResponse> getCart(@PathVariable UUID sessionId) {
        return ApiResponse.success(cartService.getCart(sessionId));
    }

    @PatchMapping("/{sessionId}/items/{productId}")
    public ApiResponse<AddItemResponse> updateItem(
            @PathVariable UUID sessionId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateItemQuantity(sessionId, productId, request));
    }

    @DeleteMapping("/{sessionId}/items/{productId}")
    public ApiResponse<CartResponse> removeItem(
            @PathVariable UUID sessionId,
            @PathVariable UUID productId) {
        return ApiResponse.success(cartService.removeItem(sessionId, productId));
    }

    @PostMapping("/{sessionId}/items/{productId}/verify-weight")
    public ApiResponse<WeightVerificationResponse> verifyWeight(
            @PathVariable UUID sessionId,
            @PathVariable UUID productId,
            @Valid @RequestBody VerifyWeightRequest request) {
        return ApiResponse.success(cartService.verifyWeight(sessionId, productId, request));
    }
}
