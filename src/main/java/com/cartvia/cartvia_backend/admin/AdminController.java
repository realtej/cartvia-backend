package com.cartvia.cartvia_backend.admin;

import com.cartvia.cartvia_backend.admin.dto.BulkDeactivateRequest;
import com.cartvia.cartvia_backend.admin.dto.BulkDeactivateResponse;
import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.common.dto.PageResponse;
import com.cartvia.cartvia_backend.common.enums.OrderStatus;
import com.cartvia.cartvia_backend.inventory.dto.InventoryResponse;
import com.cartvia.cartvia_backend.order.dto.OrderResponse;
import com.cartvia.cartvia_backend.product.dto.ProductResponse;
import com.cartvia.cartvia_backend.trolley.dto.TrolleyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/trolleys")
    public ApiResponse<List<TrolleyResponse>> listTrolleys(@RequestParam UUID storeId) {
        return ApiResponse.success(adminService.listTrolleys(storeId));
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductResponse>> listProducts(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.listProducts(storeId, search, category, page, size));
    }

    @PostMapping("/products/bulk-deactivate")
    public ApiResponse<BulkDeactivateResponse> bulkDeactivate(@Valid @RequestBody BulkDeactivateRequest request) {
        return ApiResponse.success(adminService.bulkDeactivate(request));
    }

    @GetMapping("/stock")
    public ApiResponse<List<InventoryResponse>> listStock(@RequestParam UUID storeId) {
        return ApiResponse.success(adminService.listStock(storeId));
    }

    @GetMapping("/stock/low")
    public ApiResponse<List<InventoryResponse>> listLowStock(@RequestParam UUID storeId) {
        return ApiResponse.success(adminService.listLowStock(storeId));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<OrderResponse>> listOrders(
            @RequestParam UUID storeId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.listOrders(storeId, status, page, size));
    }
}
