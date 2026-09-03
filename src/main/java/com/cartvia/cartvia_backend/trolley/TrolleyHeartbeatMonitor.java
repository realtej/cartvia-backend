package com.cartvia.cartvia_backend.trolley;

import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;
import com.cartvia.cartvia_backend.config.CartviaProperties;
import com.cartvia.cartvia_backend.trolley.entity.Trolley;
import com.cartvia.cartvia_backend.trolley.repository.TrolleyRepository;
import com.cartvia.cartvia_backend.websocket.WebSocketEventPublisher;
import com.cartvia.cartvia_backend.websocket.dto.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nothing previously read cartvia.trolley.heartbeat-timeout-seconds — it was
 * declared in CartviaProperties but unused. This is what it's for: a trolley
 * that stops heartbeating (POST /api/trolleys/{code}/heartbeat) for longer
 * than the configured timeout is presumed disconnected (powered off, out of
 * range, crashed) and flipped back to INACTIVE, with a TROLLEY_DISCONNECTED
 * event published so any customer app / admin dashboard watching that
 * trolley's topic finds out promptly instead of only on the next failed
 * heartbeat check from their own side.
 * <p>
 * Deliberately leaves IN_SESSION trolleys' ShoppingSession alone — a
 * connectivity blip mid-session shouldn't silently end or cancel a
 * checkout-in-progress; that's a separate concern from device connectivity.
 */
@Component
@RequiredArgsConstructor
public class TrolleyHeartbeatMonitor {

    private final TrolleyRepository trolleyRepository;
    private final CartviaProperties cartviaProperties;
    private final WebSocketEventPublisher webSocketEventPublisher;

    @Scheduled(fixedDelayString = "30000")
    @Transactional
    public void checkForDisconnectedTrolleys() {
        int timeoutSeconds = cartviaProperties.getTrolley().getHeartbeatTimeoutSeconds();
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);

        List<Trolley> silent = trolleyRepository.findByStatusInAndLastSeenAtBefore(
                List.of(TrolleyStatus.ACTIVE, TrolleyStatus.IN_SESSION), cutoff);

        for (Trolley trolley : silent) {
            trolley.setStatus(TrolleyStatus.INACTIVE);
            trolleyRepository.save(trolley);
            webSocketEventPublisher.toTrolley(trolley.getTrolleyCode(), WebSocketEventType.TROLLEY_DISCONNECTED,
                    trolley.getTrolleyCode());
        }
    }
}
