package com.cartvia.cartvia_backend.notification;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.notification.dto.NotificationResponse;
import com.cartvia.cartvia_backend.notification.entity.Notification;
import com.cartvia.cartvia_backend.notification.repository.NotificationRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Per Authorization Spec §7.3: the existing contract (GET
 * /api/notifications?userId=) trusted a client-supplied userId, which would
 * let Customer A read Customer B's notifications. We take the spec's
 * preferred fix — derive the user from the JWT instead of accepting a query
 * param at all — rather than the fallback of validating a passed-in userId.
 * Role matrix (§ role table) is CUSTOMER-only for both endpoints, no ADMIN
 * bypass.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications(boolean unreadOnly) {
        authorizationService.requireRole(Role.CUSTOMER);
        UUID userId = authenticationService.getAuthenticatedUserId();

        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUser_IdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId);

        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId) {
        authorizationService.requireRole(Role.CUSTOMER);
        authorizationService.requireCustomerOwnsNotification(notificationId, authenticationService.getAuthenticatedUserId());

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Notification not found"));

        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
