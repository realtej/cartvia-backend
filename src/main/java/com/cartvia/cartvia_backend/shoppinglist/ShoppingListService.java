package com.cartvia.cartvia_backend.shoppinglist;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.product.repository.ProductRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.shoppinglist.dto.AddShoppingListItemRequest;
import com.cartvia.cartvia_backend.shoppinglist.dto.ShoppingListItemDto;
import com.cartvia.cartvia_backend.shoppinglist.dto.ShoppingListResponse;
import com.cartvia.cartvia_backend.shoppinglist.dto.UpdateShoppingListItemRequest;
import com.cartvia.cartvia_backend.shoppinglist.entity.ShoppingList;
import com.cartvia.cartvia_backend.shoppinglist.entity.ShoppingListItem;
import com.cartvia.cartvia_backend.shoppinglist.repository.ShoppingListItemRepository;
import com.cartvia.cartvia_backend.shoppinglist.repository.ShoppingListRepository;
import com.cartvia.cartvia_backend.user.entity.User;
import com.cartvia.cartvia_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Per Authorization Spec §7.4: verify ShoppingList.userId ==
 * authenticatedUserId. Unlike Notifications (§7.3), the spec here doesn't
 * offer "derive from JWT instead" as a preferred alternative — the JSON
 * body contract (§4.11) keeps userId as a required query param for
 * get/create — so we keep the documented query param but enforce it via
 * AuthorizationService.requireUserIdMatchesAuthenticated, matching what
 * §7.4 actually asks for.
 * <p>
 * A user may have multiple ShoppingList rows over time (repository already
 * had findFirstByUser_IdOrderByCreatedAtDesc); GET always resolves to the
 * most recently created one, auto-creating an empty one on first use so a
 * brand-new customer isn't met with a 404. POST always creates a new list
 * (per spec 11.2, "creates and returns a new/empty list"), which then
 * becomes the current one for subsequent GETs.
 */
@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    @Transactional
    public ShoppingListResponse getOrCreateList(UUID userId) {
        authorizationService.requireUserIdMatchesAuthenticated(userId);
        User user = requireUser(userId);

        ShoppingList list = shoppingListRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)
                .orElseGet(() -> createListForUser(user));

        return toResponse(list);
    }

    @Transactional
    public ShoppingListResponse createList(UUID userId) {
        authorizationService.requireUserIdMatchesAuthenticated(userId);
        User user = requireUser(userId);
        return toResponse(createListForUser(user));
    }

    @Transactional
    public ShoppingListResponse addItem(UUID listId, AddShoppingListItemRequest request) {
        authorizationService.requireCustomerOwnsShoppingList(listId, authenticationService.getAuthenticatedUserId());

        boolean hasProduct = request.getProductId() != null;
        boolean hasCustomName = request.getCustomName() != null && !request.getCustomName().isBlank();
        if (hasProduct == hasCustomName) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Provide exactly one of productId or customName");
        }

        ShoppingList list = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Shopping list not found"));

        ShoppingListItem item = new ShoppingListItem();
        item.setShoppingList(list);
        if (hasProduct) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
            item.setProduct(product);
        } else {
            item.setCustomName(request.getCustomName());
        }
        shoppingListItemRepository.save(item);

        return toResponse(list);
    }

    @Transactional
    public ShoppingListResponse updateItem(UUID itemId, UpdateShoppingListItemRequest request) {
        authorizationService.requireCustomerOwnsShoppingListItem(itemId, authenticationService.getAuthenticatedUserId());

        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Shopping list item not found"));

        item.setPurchased(request.getPurchased());
        item.setPurchasedAt(request.getPurchased() ? LocalDateTime.now() : null);
        shoppingListItemRepository.save(item);

        return toResponse(item.getShoppingList());
    }

    private ShoppingList createListForUser(User user) {
        ShoppingList list = new ShoppingList();
        list.setUser(user);
        return shoppingListRepository.save(list);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
    }

    private ShoppingListResponse toResponse(ShoppingList list) {
        List<ShoppingListItemDto> items = shoppingListItemRepository.findByShoppingList_Id(list.getId()).stream()
                .map(this::toItemDto)
                .toList();
        return new ShoppingListResponse(list.getId(), list.getName(), items);
    }

    private ShoppingListItemDto toItemDto(ShoppingListItem item) {
        String name = item.getProduct() != null ? item.getProduct().getName() : item.getCustomName();
        return new ShoppingListItemDto(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                name,
                item.isPurchased(),
                item.getPurchasedAt());
    }
}
