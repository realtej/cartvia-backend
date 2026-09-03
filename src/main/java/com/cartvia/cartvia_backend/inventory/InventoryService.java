package com.cartvia.cartvia_backend.inventory;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.inventory.dto.AdjustInventoryRequest;
import com.cartvia.cartvia_backend.inventory.dto.InventoryResponse;
import com.cartvia.cartvia_backend.inventory.entity.Inventory;
import com.cartvia.cartvia_backend.inventory.repository.InventoryRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.store.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StoreService storeService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<InventoryResponse> listInventory(UUID storeId) {
        UUID resolvedStoreId = requireAccessibleStoreId(storeId);
        authorizationService.requireStoreStaffForStore(resolvedStoreId);
        return inventoryRepository.findByStore_Id(resolvedStoreId).stream()
                .sorted(Comparator.comparing(inv -> inv.getProduct().getName()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listLowStock(UUID storeId) {
        UUID resolvedStoreId = requireAccessibleStoreId(storeId);
        authorizationService.requireStoreStaffForStore(resolvedStoreId);
        return inventoryRepository.findByStore_Id(resolvedStoreId).stream()
                .filter(inv -> inv.getStockQty() <= inv.getLowStockThreshold())
                .sorted(Comparator.comparingInt(Inventory::getStockQty))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryResponse adjustInventory(UUID productId, UUID storeId, AdjustInventoryRequest request) {
        UUID resolvedStoreId = resolveStoreIdForAdjust(storeId);
        authorizationService.requireStoreStaffForStore(resolvedStoreId);

        Inventory inventory = inventoryRepository.findByStore_IdAndProduct_Id(resolvedStoreId, productId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "No inventory row for this product"));

        int newQty = inventory.getStockQty() + request.getDelta();
        if (newQty < 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Stock quantity cannot be negative");
        }
        inventory.setStockQty(newQty);
        inventory = inventoryRepository.save(inventory);
        return toResponse(inventory);
    }

    private UUID resolveStoreIdForAdjust(UUID storeId) {
        if (authenticationService.getAuthenticatedRole() == Role.STORE_STAFF) {
            return authenticationService.getAuthenticatedStoreId();
        }
        if (storeId == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "storeId is required");
        }
        storeService.requireStore(storeId);
        return storeId;
    }

    private UUID requireAccessibleStoreId(UUID storeId) {
        if (storeId == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "storeId is required");
        }
        if (authenticationService.getAuthenticatedRole() == Role.STORE_STAFF
                && !authenticationService.getAuthenticatedStoreId().equals(storeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                    "You do not have permission to perform this operation");
        }
        storeService.requireStore(storeId);
        return storeId;
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getProduct().getName(),
                inventory.getStore().getId(),
                inventory.getStockQty(),
                inventory.getLowStockThreshold(),
                inventory.getUpdatedAt());
    }
}
