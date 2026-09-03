package com.cartvia.cartvia_backend.notification.repository;

import com.cartvia.cartvia_backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUser_IdAndReadFalseOrderByCreatedAtDesc(UUID userId);
}
