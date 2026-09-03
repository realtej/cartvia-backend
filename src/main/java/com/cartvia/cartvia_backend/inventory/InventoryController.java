package com.cartvia.cartvia_backend.inventory;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.inventory.dto.AdjustInventoryRequest;
import com.cartvia.cartvia_backend.inventory.dto.InventoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ApiResponse<List<InventoryResponse>> listInventory(@RequestParam UUID storeId) {
        return ApiResponse.success(inventoryService.listInventory(storeId));
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<InventoryResponse>> listLowStock(@RequestParam UUID storeId) {
        return ApiResponse.success(inventoryService.listLowStock(storeId));
    }

    @PatchMapping("/{productId}/adjust")
    public ApiResponse<InventoryResponse> adjustInventory(
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID storeId,
            @Valid @RequestBody AdjustInventoryRequest request) {
        return ApiResponse.success(inventoryService.adjustInventory(productId, storeId, request));
    }
}
