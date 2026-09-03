package com.cartvia.cartvia_backend.order;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.order.dto.OrderItemDto;
import com.cartvia.cartvia_backend.order.dto.OrderResponse;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.entity.OrderItem;
import com.cartvia.cartvia_backend.order.repository.OrderRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    /**
     * Order creation happens transactionally as part of session checkout
     * (see SessionService#endSession) — never directly via a client-facing
     * "create order" endpoint, since price and totals must always be
     * server-derived from the cart at checkout time.
     * <p>
     * This service only exposes read access. Per the Authorization Spec role
     * matrix for GET /api/orders/{id}: CUSTOMER may only look up their own
     * order (ownership check, §7.2), ADMIN has unrestricted read access,
     * and STORE_STAFF/DEVICE/PUBLIC are denied entirely (staff use the
     * store-scoped /api/admin/orders listing instead).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Role role = authenticationService.getAuthenticatedRole();

        if (role == Role.ADMIN) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));
            return toResponse(order);
        }

        authorizationService.requireRole(Role.CUSTOMER);
        UUID authenticatedUserId = authenticationService.getAuthenticatedUserId();
        authorizationService.requireCustomerOwnsOrder(orderId, authenticatedUserId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(this::toItemDto)
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

    private OrderItemDto toItemDto(OrderItem item) {
        return new OrderItemDto(
                item.getProduct().getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
