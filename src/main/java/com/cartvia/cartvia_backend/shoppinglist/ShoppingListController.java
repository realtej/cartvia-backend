package com.cartvia.cartvia_backend.shoppinglist;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.shoppinglist.dto.AddShoppingListItemRequest;
import com.cartvia.cartvia_backend.shoppinglist.dto.ShoppingListResponse;
import com.cartvia.cartvia_backend.shoppinglist.dto.UpdateShoppingListItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/shopping-list")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    @GetMapping
    public ApiResponse<ShoppingListResponse> getList(@RequestParam UUID userId) {
        return ApiResponse.success(shoppingListService.getOrCreateList(userId));
    }

    @PostMapping
    public ApiResponse<ShoppingListResponse> createList(@RequestParam UUID userId) {
        return ApiResponse.success(shoppingListService.createList(userId));
    }

    @PostMapping("/{listId}/items")
    public ApiResponse<ShoppingListResponse> addItem(
            @PathVariable UUID listId,
            @RequestBody AddShoppingListItemRequest request) {
        return ApiResponse.success(shoppingListService.addItem(listId, request));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<ShoppingListResponse> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateShoppingListItemRequest request) {
        return ApiResponse.success(shoppingListService.updateItem(itemId, request));
    }
}
