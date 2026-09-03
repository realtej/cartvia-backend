package com.cartvia.cartvia_backend.security;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.inventory.entity.Inventory;
import com.cartvia.cartvia_backend.inventory.repository.InventoryRepository;
import com.cartvia.cartvia_backend.notification.entity.Notification;
import com.cartvia.cartvia_backend.notification.repository.NotificationRepository;
import com.cartvia.cartvia_backend.offer.entity.Offer;
import com.cartvia.cartvia_backend.offer.repository.OfferRepository;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.repository.OrderRepository;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.product.repository.ProductRepository;
import com.cartvia.cartvia_backend.session.entity.ShoppingSession;
import com.cartvia.cartvia_backend.session.repository.ShoppingSessionRepository;
import com.cartvia.cartvia_backend.shoppinglist.entity.ShoppingList;
import com.cartvia.cartvia_backend.shoppinglist.entity.ShoppingListItem;
import com.cartvia.cartvia_backend.shoppinglist.repository.ShoppingListItemRepository;
import com.cartvia.cartvia_backend.shoppinglist.repository.ShoppingListRepository;
import com.cartvia.cartvia_backend.trolley.entity.Trolley;
import com.cartvia.cartvia_backend.trolley.repository.TrolleyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final ShoppingSessionRepository sessionRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final NotificationRepository notificationRepository;
    private final OfferRepository offerRepository;
    private final TrolleyRepository trolleyRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final AuthenticationService authenticationService;

    public void requireCustomerOwnsSession(UUID sessionId, UUID authenticatedUserId) {
        ShoppingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND, HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getUser().getId().equals(authenticatedUserId)) {
            throw forbidden();
        }
    }

    public void requireCustomerOwnsOrder(UUID orderId, UUID authenticatedUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(authenticatedUserId)) {
            throw forbidden();
        }
    }

    public void requireCustomerOwnsShoppingList(UUID listId, UUID authenticatedUserId) {
        ShoppingList list = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Shopping list not found"));
        if (!list.getUser().getId().equals(authenticatedUserId)) {
            throw forbidden();
        }
    }

    public void requireCustomerOwnsShoppingListItem(UUID itemId, UUID authenticatedUserId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Shopping list item not found"));
        if (!item.getShoppingList().getUser().getId().equals(authenticatedUserId)) {
            throw forbidden();
        }
    }

    public void requireCustomerOwnsNotification(UUID notificationId, UUID authenticatedUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getUser().getId().equals(authenticatedUserId)) {
            throw forbidden();
        }
    }

    public void requireUserIdMatchesAuthenticated(UUID requestedUserId) {
        if (!authenticationService.getAuthenticatedUserId().equals(requestedUserId)) {
            throw forbidden();
        }
    }

    public void requireStoreStaffForStore(UUID resourceStoreId) {
        requireRole(Role.STORE_STAFF, Role.ADMIN);
        if (authenticationService.getAuthenticatedRole() == Role.ADMIN) {
            return;
        }
        if (!authenticationService.getAuthenticatedStoreId().equals(resourceStoreId)) {
            throw forbidden();
        }
    }

    public void requireStoreStaffForProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
        if (product.getStore() == null) {
            if (authenticationService.getAuthenticatedRole() != Role.ADMIN) {
                throw forbidden();
            }
            return;
        }
        requireStoreStaffForStore(product.getStore().getId());
    }

    public void requireStoreStaffForInventory(UUID storeId, UUID productId) {
        Inventory inventory = inventoryRepository.findByStore_IdAndProduct_Id(storeId, productId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "No inventory row for this product"));
        requireStoreStaffForStore(inventory.getStore().getId());
    }

    public void requireStoreStaffForOffer(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Offer not found"));
        requireStoreStaffForStore(offer.getStore().getId());
    }

    public void requireStoreStaffForTrolley(UUID trolleyId) {
        Trolley trolley = trolleyRepository.findById(trolleyId)
                .orElseThrow(() -> new ApiException(ErrorCode.TROLLEY_NOT_FOUND, HttpStatus.NOT_FOUND, "Trolley not found"));
        if (trolley.getStore() == null) {
            if (authenticationService.getAuthenticatedRole() != Role.ADMIN) {
                throw forbidden();
            }
            return;
        }
        requireStoreStaffForStore(trolley.getStore().getId());
    }

    public void requireStoreStaffForTrolleyCode(String trolleyCode) {
        Trolley trolley = trolleyRepository.findByTrolleyCode(trolleyCode)
                .orElseThrow(() -> new ApiException(ErrorCode.TROLLEY_NOT_FOUND, HttpStatus.NOT_FOUND, "Trolley not found"));
        if (trolley.getStore() == null) {
            if (authenticationService.getAuthenticatedRole() != Role.ADMIN) {
                throw forbidden();
            }
            return;
        }
        requireStoreStaffForStore(trolley.getStore().getId());
    }

    public void requireDeviceForSession(DevicePrincipal device, UUID sessionId) {
        ShoppingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND, HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getTrolley().getId().equals(device.getTrolleyId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                    "Device token is not authorized for this session's trolley");
        }
    }

    public void requireRole(Role... roles) {
        Role current = authenticationService.getAuthenticatedRole();
        for (Role role : roles) {
            if (current == role) {
                return;
            }
        }
        throw forbidden();
    }

    public void requireAdmin() {
        requireRole(Role.ADMIN);
    }

    private ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                "You do not have permission to perform this operation");
    }
}
