package com.cartvia.cartvia_backend.admin;

import com.cartvia.cartvia_backend.admin.dto.BulkDeactivateRequest;
import com.cartvia.cartvia_backend.admin.dto.BulkDeactivateResponse;
import com.cartvia.cartvia_backend.common.dto.PageResponse;
import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.OrderStatus;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.inventory.InventoryService;
import com.cartvia.cartvia_backend.inventory.dto.InventoryResponse;
import com.cartvia.cartvia_backend.order.dto.OrderItemDto;
import com.cartvia.cartvia_backend.order.dto.OrderResponse;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.entity.OrderItem;
import com.cartvia.cartvia_backend.order.repository.OrderRepository;
import com.cartvia.cartvia_backend.product.ProductService;
import com.cartvia.cartvia_backend.product.dto.ProductResponse;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.product.repository.ProductRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.store.StoreService;
import com.cartvia.cartvia_backend.trolley.dto.TrolleyResponse;
import com.cartvia.cartvia_backend.trolley.entity.Trolley;
import com.cartvia.cartvia_backend.trolley.repository.TrolleyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TrolleyRepository trolleyRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final StoreService storeService;

    @Transactional(readOnly = true)
    public List<TrolleyResponse> listTrolleys(UUID storeId) {
        authorizationService.requireStoreStaffForStore(requireAccessibleStoreId(storeId));
        return trolleyRepository.findByStore_Id(storeId).stream()
                .map(this::toTrolleyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(UUID storeId, String search, String category,
                                                      int page, int size) {
        return productService.listProducts(storeId, search, category, page, size, true);
    }

    @Transactional
    public BulkDeactivateResponse bulkDeactivate(BulkDeactivateRequest request) {
        authorizationService.requireAdmin();
        int succeeded = 0;
        List<UUID> failedIds = new ArrayList<>();
        for (UUID productId : request.getProductIds()) {
            var productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setActive(false);
                productRepository.save(product);
                succeeded++;
            } else {
                failedIds.add(productId);
            }
        }
        return new BulkDeactivateResponse(request.getProductIds().size(), succeeded, failedIds);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listStock(UUID storeId) {
        return inventoryService.listInventory(storeId);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listLowStock(UUID storeId) {
        return inventoryService.listLowStock(storeId);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listOrders(UUID storeId, OrderStatus status, int page, int size) {
        UUID resolvedStoreId = requireAccessibleStoreId(storeId);
        authorizationService.requireStoreStaffForStore(resolvedStoreId);
        Pageable pageable = PageRequest.of(page, clampSize(size));
        Page<Order> orders = status != null
                ? orderRepository.findByStore_IdAndStatus(resolvedStoreId, status, pageable)
                : orderRepository.findByStore_Id(resolvedStoreId, pageable);
        return PageResponse.from(orders.map(this::toOrderResponse));
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

    private TrolleyResponse toTrolleyResponse(Trolley trolley) {
        UUID storeId = trolley.getStore() != null ? trolley.getStore().getId() : null;
        String qrPayload = "CARTREX:TROLLEY:" + trolley.getTrolleyCode() + ":" + trolley.getId();
        return new TrolleyResponse(
                trolley.getId(),
                trolley.getTrolleyCode(),
                storeId,
                trolley.getStatus(),
                qrPayload,
                trolley.getLastSeenAt());
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(this::toOrderItemDto)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getTaxTotal(),
                order.getGrandTotal(),
                items,
                order.getCreatedAt());
    }

    private OrderItemDto toOrderItemDto(OrderItem item) {
        return new OrderItemDto(
                item.getProduct().getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }

    private int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, 100);
    }
}
