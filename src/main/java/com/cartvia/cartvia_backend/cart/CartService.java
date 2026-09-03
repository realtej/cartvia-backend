package com.cartvia.cartvia_backend.cart;

import com.cartvia.cartvia_backend.cart.dto.AddCartItemRequest;
import com.cartvia.cartvia_backend.cart.dto.AddItemResponse;
import com.cartvia.cartvia_backend.cart.dto.BatchAddRequest;
import com.cartvia.cartvia_backend.cart.dto.BatchCartItemRequest;
import com.cartvia.cartvia_backend.cart.dto.CartItemDto;
import com.cartvia.cartvia_backend.cart.dto.CartResponse;
import com.cartvia.cartvia_backend.cart.dto.UpdateCartItemRequest;
import com.cartvia.cartvia_backend.cart.dto.VerifyWeightRequest;
import com.cartvia.cartvia_backend.cart.dto.WeightVerificationResponse;
import com.cartvia.cartvia_backend.cart.entity.Cart;
import com.cartvia.cartvia_backend.cart.entity.CartItem;
import com.cartvia.cartvia_backend.cart.repository.CartItemRepository;
import com.cartvia.cartvia_backend.common.PricingService;
import com.cartvia.cartvia_backend.common.enums.CartSource;
import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.VerificationStatus;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.inventory.repository.InventoryRepository;
import com.cartvia.cartvia_backend.notification.entity.Notification;
import com.cartvia.cartvia_backend.notification.repository.NotificationRepository;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.product.repository.ProductRepository;
import com.cartvia.cartvia_backend.recommendation.RecommendationService;
import com.cartvia.cartvia_backend.recommendation.dto.RecommendationDto;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.session.SessionService;
import com.cartvia.cartvia_backend.session.entity.ShoppingSession;
import com.cartvia.cartvia_backend.websocket.WebSocketEventPublisher;
import com.cartvia.cartvia_backend.websocket.dto.ProductScannedDto;
import com.cartvia.cartvia_backend.websocket.dto.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final NotificationRepository notificationRepository;
    private final PricingService pricingService;
    private final SessionService sessionService;
    private final RecommendationService recommendationService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final WebSocketEventPublisher webSocketEventPublisher;

    @Transactional
    public AddItemResponse addItem(UUID sessionId, AddCartItemRequest request) {
        ShoppingSession session = sessionService.requireAccessibleActiveSession(sessionId);
        Product product = resolveProduct(request.getBarcode(), session.getStore().getId());
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.PRODUCT_SCANNED, new ProductScannedDto(product.getId(), product.getName(), product.getBarcode()));
        CartItem item = addOrIncrementItem(session, product, request.getSource(), null);
        BigDecimal cartTotal = recalculateAndGetTotal(session);
        CartItemDto itemDto = toItemDto(item, false);
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.PRODUCT_ADDED, itemDto);
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.CART_UPDATED, cartTotal);
        List<RecommendationDto> recommendations = recommendationService.getFrequentlyBoughtTogether(product.getId());
        return new AddItemResponse(
                session.getCart().getId(),
                itemDto,
                cartTotal,
                recommendations);
    }

    @Transactional
    public CartResponse batchAddItems(UUID sessionId, BatchAddRequest request) {
        ShoppingSession session = sessionService.requireAccessibleActiveSession(sessionId);
        for (BatchCartItemRequest batchItem : request.getItems()) {
            Product product = resolveProduct(batchItem.getBarcode(), session.getStore().getId());
            webSocketEventPublisher.toSession(sessionId, WebSocketEventType.PRODUCT_SCANNED, new ProductScannedDto(product.getId(), product.getName(), product.getBarcode()));
            CartItem item = addOrIncrementItem(session, product, batchItem.getSource(), batchItem.getClientTs());
            webSocketEventPublisher.toSession(sessionId, WebSocketEventType.PRODUCT_ADDED, toItemDto(item, false));
        }
        CartResponse cartResponse = getCart(sessionId);
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.CART_UPDATED, cartResponse.totalAmount());
        return cartResponse;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID sessionId) {
        ShoppingSession session = sessionService.requireAccessibleActiveSession(sessionId);
        return buildCartResponse(session);
    }

    @Transactional
    public AddItemResponse updateItemQuantity(UUID sessionId, UUID productId, UpdateCartItemRequest request) {
        ShoppingSession session = requireCustomerSession(sessionId);
        CartItem item = findCartItem(session, productId);
        item.setQuantity(request.getQuantity());
        item.setVerificationStatus(VerificationStatus.PENDING);
        cartItemRepository.save(item);
        BigDecimal cartTotal = recalculateAndGetTotal(session);
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.CART_UPDATED, cartTotal);
        return new AddItemResponse(session.getCart().getId(), toItemDto(item, false), cartTotal, List.of());
    }

    @Transactional
    public CartResponse removeItem(UUID sessionId, UUID productId) {
        ShoppingSession session = requireCustomerSession(sessionId);
        cartItemRepository.deleteByCart_IdAndProduct_Id(session.getCart().getId(), productId);
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.PRODUCT_REMOVED, productId);
        CartResponse cartResponse = buildCartResponse(session);
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.CART_UPDATED, cartResponse.totalAmount());
        return cartResponse;
    }

    @Transactional
    public WeightVerificationResponse verifyWeight(UUID sessionId, UUID productId, VerifyWeightRequest request) {
        ShoppingSession session = sessionService.requireAccessibleActiveSession(sessionId);
        CartItem item = findCartItem(session, productId);
        Product product = item.getProduct();

        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.WEIGHT_UPDATED, request.getMeasuredWeightG());

        BigDecimal expectedWeight = product.getExpectedWeightG();
        if (expectedWeight == null) {
            item.setVerificationStatus(VerificationStatus.VERIFIED);
            item.setMeasuredWeightG(request.getMeasuredWeightG());
            cartItemRepository.save(item);
            WeightVerificationResponse result = new WeightVerificationResponse(productId, VerificationStatus.VERIFIED,
                    request.getMeasuredWeightG(), null);
            webSocketEventPublisher.toSession(sessionId, WebSocketEventType.WEIGHT_VALIDATION_RESULT, result);
            return result;
        }

        BigDecimal expectedTotal = expectedWeight.multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal tolerancePct = product.getWeightTolerancePct() != null
                ? product.getWeightTolerancePct()
                : BigDecimal.valueOf(3);
        BigDecimal tolerance = expectedTotal.multiply(tolerancePct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lower = expectedTotal.subtract(tolerance);
        BigDecimal upper = expectedTotal.add(tolerance);

        VerificationStatus status = request.getMeasuredWeightG().compareTo(lower) >= 0
                && request.getMeasuredWeightG().compareTo(upper) <= 0
                ? VerificationStatus.VERIFIED
                : VerificationStatus.MISMATCH;

        item.setVerificationStatus(status);
        item.setMeasuredWeightG(request.getMeasuredWeightG());
        cartItemRepository.save(item);

        if (status == VerificationStatus.MISMATCH) {
            createWeightMismatchNotification(session, product);
        }

        WeightVerificationResponse result = new WeightVerificationResponse(productId, status, request.getMeasuredWeightG(), expectedTotal);
        webSocketEventPublisher.toSession(sessionId, WebSocketEventType.WEIGHT_VALIDATION_RESULT, result);
        return result;
    }

    private ShoppingSession requireCustomerSession(UUID sessionId) {
        authorizationService.requireCustomerOwnsSession(sessionId, authenticationService.getAuthenticatedUserId());
        return sessionService.requireActiveSession(sessionId);
    }

    private Product resolveProduct(String barcode, UUID storeId) {
        Product product = productRepository.findByBarcode(barcode)
                .filter(Product::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Product not found for barcode " + barcode));

        inventoryRepository.findByStore_IdAndProduct_Id(storeId, product.getId())
                .filter(inv -> inv.getStockQty() > 0)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT,
                        "Product is out of stock"));

        return product;
    }

    private CartItem addOrIncrementItem(ShoppingSession session, Product product, CartSource source,
                                        java.time.LocalDateTime clientTs) {
        Cart cart = session.getCart();
        CartItem item = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId())
                .orElse(null);

        BigDecimal unitPrice = pricingService.getEffectiveUnitPrice(product, session.getStore().getId());

        if (item != null) {
            item.setQuantity(item.getQuantity() + 1);
            item.setUnitPrice(unitPrice);
            item.setVerificationStatus(VerificationStatus.PENDING);
            if (source != null) {
                item.setSource(source);
            }
            if (clientTs != null) {
                item.setClientTs(clientTs);
            }
            return cartItemRepository.save(item);
        }

        item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(unitPrice);
        item.setSource(source != null ? source : CartSource.SCAN);
        item.setVerificationStatus(VerificationStatus.PENDING);
        item.setClientTs(clientTs);
        return cartItemRepository.save(item);
    }

    private CartItem findCartItem(ShoppingSession session, UUID productId) {
        return cartItemRepository.findByCart_IdAndProduct_Id(session.getCart().getId(), productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
    }

    private BigDecimal recalculateAndGetTotal(ShoppingSession session) {
        List<CartItem> items = cartItemRepository.findByCart_Id(session.getCart().getId());
        return pricingService.calculateCartSubtotal(items);
    }

    private CartResponse buildCartResponse(ShoppingSession session) {
        List<CartItem> items = cartItemRepository.findByCart_Id(session.getCart().getId());
        List<CartItemDto> itemDtos = items.stream()
                .map(item -> toItemDto(item, true))
                .toList();
        BigDecimal total = pricingService.calculateCartSubtotal(items);
        return new CartResponse(session.getCart().getId(), session.getId(), total, itemDtos);
    }

    private CartItemDto toItemDto(CartItem item, boolean includeVerification) {
        if (includeVerification) {
            return new CartItemDto(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getVerificationStatus());
        }
        return new CartItemDto(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice());
    }

    private void createWeightMismatchNotification(ShoppingSession session, Product product) {
        Notification notification = new Notification();
        notification.setUser(session.getUser());
        notification.setType("WEIGHT_MISMATCH");
        notification.setTitle("Weight mismatch detected");
        notification.setBody(product.getName() + " in your cart shows a weight mismatch. Please recheck the item.");
        notification.setRead(false);
        notificationRepository.save(notification);
    }
}
