package com.cartvia.cartvia_backend.notification;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // No userId query param, by design — see NotificationService javadoc
    // and Authorization Spec §7.3. unreadOnly is a plain filter, not an
    // identity input, so it stays.
    @GetMapping
    public ApiResponse<List<NotificationResponse>> listNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        return ApiResponse.success(notificationService.listNotifications(unreadOnly));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID id) {
        return ApiResponse.success(notificationService.markRead(id));
    }
}
