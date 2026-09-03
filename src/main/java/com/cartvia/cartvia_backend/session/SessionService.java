package com.cartvia.cartvia_backend.session;

import com.cartvia.cartvia_backend.cart.entity.Cart;
import com.cartvia.cartvia_backend.cart.entity.CartItem;
import com.cartvia.cartvia_backend.cart.repository.CartItemRepository;
import com.cartvia.cartvia_backend.cart.repository.CartRepository;
import com.cartvia.cartvia_backend.common.PricingService;
import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.OrderStatus;
import com.cartvia.cartvia_backend.common.enums.SessionStatus;
import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.inventory.entity.Inventory;
import com.cartvia.cartvia_backend.inventory.repository.InventoryRepository;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.entity.OrderItem;
import com.cartvia.cartvia_backend.order.repository.OrderRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.session.dto.CreateSessionRequest;
import com.cartvia.cartvia_backend.session.dto.EndSessionResponse;
import com.cartvia.cartvia_backend.session.dto.SessionResponse;
import com.cartvia.cartvia_backend.session.entity.ShoppingSession;
import com.cartvia.cartvia_backend.session.repository.ShoppingSessionRepository;
import com.cartvia.cartvia_backend.store.entity.Store;
import com.cartvia.cartvia_backend.trolley.entity.Trolley;
import com.cartvia.cartvia_backend.trolley.repository.TrolleyRepository;
import com.cartvia.cartvia_backend.user.entity.User;
import com.cartvia.cartvia_backend.user.repository.UserRepository;
import com.cartvia.cartvia_backend.websocket.WebSocketEventPublisher;
import com.cartvia.cartvia_backend.websocket.dto.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final ShoppingSessionRepository sessionRepository;
    private final TrolleyRepository trolleyRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingService pricingService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final WebSocketEventPublisher webSocketEventPublisher;

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        UUID userId = authenticationService.getAuthenticatedUserId();
        if (request.getUserId() != null && !request.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                    "You do not have permission to perform this operation");
        }

        Trolley trolley = trolleyRepository.findByTrolleyCode(request.getTrolleyCode())
                .orElseThrow(() -> new ApiException(ErrorCode.TROLLEY_NOT_FOUND, HttpStatus.NOT_FOUND, "Trolley not found"));

        if (trolley.getStore() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Trolley is not assigned to a store");
        }
        if (trolley.getStatus() == TrolleyStatus.MAINTENANCE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT, "Trolley is not available");
        }

        sessionRepository.findByTrolley_IdAndStatus(trolley.getId(), SessionStatus.ACTIVE)
                .ifPresent(s -> {
                    throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT,
                            "Trolley is already in an active session");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));

        Cart cart = new Cart();
        cart = cartRepository.save(cart);

        ShoppingSession session = new ShoppingSession();
        session.setSessionCode(generateSessionCode(trolley));
        session.setUser(user);
        session.setTrolley(trolley);
        session.setStore(trolley.getStore());
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        cart.setSession(session);
        cartRepository.save(cart);

        trolley.setStatus(TrolleyStatus.IN_SESSION);
        trolleyRepository.save(trolley);

        SessionResponse sessionResponse = toResponse(session, BigDecimal.ZERO);
        webSocketEventPublisher.toSession(session.getId(), WebSocketEventType.SESSION_UPDATED, sessionResponse);
        return sessionResponse;
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId) {
        ShoppingSession session = requireOwnedSession(sessionId);
        return toResponse(session, calculateCartTotal(session));
    }

    @Transactional
    public EndSessionResponse endSession(UUID sessionId) {
        ShoppingSession session = requireOwnedSession(sessionId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT, "Session is not active");
        }

        List<CartItem> items = cartItemRepository.findByCart_Id(session.getCart().getId());
        if (items.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.CHECKOUT_STARTED, sessionId);

        // Re-validate stock at checkout time: quantities may have been depleted by other
        // sessions/admin adjustments since items were added to this cart (add-to-cart only
        // checked stockQty > 0, not that enough units remain for the requested quantity).
        validateInventoryForCheckout(items, session.getStore().getId());

        PricingService.OrderTotals totals = pricingService.calculateOrderTotals(items, session.getStore().getId());

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUser(session.getUser());
        order.setSession(session);
        order.setStore(session.getStore());
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(totals.subtotal());
        order.setDiscountTotal(totals.discountTotal());
        order.setTaxTotal(totals.taxTotal());
        order.setGrandTotal(totals.grandTotal());
        order = orderRepository.save(order);

        for (CartItem cartItem : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setName(cartItem.getProduct().getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setLineTotal(pricingService.calculateLineTotal(cartItem.getUnitPrice(), cartItem.getQuantity()));
            order.getItems().add(orderItem);
        }
        orderRepository.save(order);

        session.setStatus(SessionStatus.CHECKED_OUT);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);

        Trolley trolley = session.getTrolley();
        trolley.setStatus(TrolleyStatus.ACTIVE);
        trolleyRepository.save(trolley);

        SessionResponse sessionResponse = toResponse(session, totals.grandTotal());
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.SESSION_UPDATED, sessionResponse);
        return new EndSessionResponse(sessionResponse, order.getId(), order.getOrderCode(), totals.grandTotal());
    }

    // Checks each cart item's current inventory row for the session's store against the
    // quantity actually in the cart, so a product that went out of stock (or dropped below
    // the cart's quantity) between add-to-cart and checkout blocks the order instead of
    // silently overselling.
    private void validateInventoryForCheckout(List<CartItem> items, UUID storeId) {
        for (CartItem item : items) {
            Inventory inventory = inventoryRepository.findByStore_IdAndProduct_Id(storeId, item.getProduct().getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT,
                            item.getProduct().getName() + " is no longer available in this store"));
            if (inventory.getStockQty() < item.getQuantity()) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT,
                        "Insufficient stock for " + item.getProduct().getName());
            }
        }
    }

    @Transactional(readOnly = true)
    public ShoppingSession requireActiveSession(UUID sessionId) {
        ShoppingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND, HttpStatus.NOT_FOUND, "Session not found"));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT, "Session is not active");
        }
        return session;
    }

    public ShoppingSession requireAccessibleActiveSession(UUID sessionId) {
        ShoppingSession session = requireActiveSession(sessionId);
        if (authenticationService.isDeviceAuthenticated()) {
            authorizationService.requireDeviceForSession(authenticationService.getDevicePrincipal(), sessionId);
        } else {
            authorizationService.requireCustomerOwnsSession(sessionId, authenticationService.getAuthenticatedUserId());
        }
        return session;
    }

    private ShoppingSession requireOwnedSession(UUID sessionId) {
        authorizationService.requireCustomerOwnsSession(sessionId, authenticationService.getAuthenticatedUserId());
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND, HttpStatus.NOT_FOUND, "Session not found"));
    }

    private BigDecimal calculateCartTotal(ShoppingSession session) {
        if (session.getCart() == null) {
            return BigDecimal.ZERO;
        }
        List<CartItem> items = cartItemRepository.findByCart_Id(session.getCart().getId());
        return pricingService.calculateCartSubtotal(items);
    }

    private SessionResponse toResponse(ShoppingSession session, BigDecimal cartTotal) {
        Cart cart = session.getCart();
        if (cart == null) {
            cart = cartRepository.findBySession_Id(session.getId()).orElse(null);
        }
        return new SessionResponse(
                session.getId(),
                session.getSessionCode(),
                session.getUser().getId(),
                session.getTrolley().getTrolleyCode(),
                session.getStore().getId(),
                session.getStatus(),
                cart != null ? cart.getId() : null,
                cartTotal,
                session.getStartedAt(),
                session.getEndedAt());
    }

    private String generateSessionCode(Trolley trolley) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = trolley.getTrolleyCode().replace("TRLY-", "");
        return "SESS-" + date + "-" + suffix;
    }

    private String generateOrderCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD-" + date + "-" + suffix;
    }
}
