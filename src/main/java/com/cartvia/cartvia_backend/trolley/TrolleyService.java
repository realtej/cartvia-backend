package com.cartvia.cartvia_backend.trolley;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.security.DeviceAuthenticationService;
import com.cartvia.cartvia_backend.security.TokenHashService;
import com.cartvia.cartvia_backend.store.StoreService;
import com.cartvia.cartvia_backend.store.entity.Store;
import com.cartvia.cartvia_backend.trolley.dto.CreateTrolleyRequest;
import com.cartvia.cartvia_backend.trolley.dto.HeartbeatRequest;
import com.cartvia.cartvia_backend.trolley.dto.TrolleyProvisionResponse;
import com.cartvia.cartvia_backend.trolley.dto.TrolleyResponse;
import com.cartvia.cartvia_backend.trolley.dto.UpdateTrolleyStatusRequest;
import com.cartvia.cartvia_backend.trolley.entity.Trolley;
import com.cartvia.cartvia_backend.trolley.repository.TrolleyRepository;
import com.cartvia.cartvia_backend.websocket.WebSocketEventPublisher;
import com.cartvia.cartvia_backend.websocket.dto.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrolleyService {

    private final TrolleyRepository trolleyRepository;
    private final StoreService storeService;
    private final TokenHashService tokenHashService;
    private final AuthorizationService authorizationService;
    private final DeviceAuthenticationService deviceAuthenticationService;
    private final WebSocketEventPublisher webSocketEventPublisher;

    @Transactional
    public TrolleyProvisionResponse createTrolley(CreateTrolleyRequest request) {
        authorizationService.requireAdmin();
        if (trolleyRepository.existsByTrolleyCode(request.getTrolleyCode())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT,
                    "A trolley with this code already exists");
        }

        Trolley trolley = new Trolley();
        trolley.setTrolleyCode(request.getTrolleyCode());
        trolley.setEsp32Mac(request.getEsp32Mac());
        trolley.setStatus(TrolleyStatus.INACTIVE);
        if (request.getStoreId() != null) {
            Store store = storeService.requireStore(request.getStoreId());
            trolley.setStore(store);
        }
        String rawDeviceToken = "dtok_" + tokenHashService.generateOpaqueToken();
        trolley.setDeviceTokenHash(tokenHashService.hashToken(rawDeviceToken));
        trolley = trolleyRepository.save(trolley);

        return new TrolleyProvisionResponse(
                trolley.getId(),
                trolley.getTrolleyCode(),
                trolley.getStore() != null ? trolley.getStore().getId() : null,
                trolley.getStatus(),
                buildQrPayload(trolley),
                rawDeviceToken);
    }

    @Transactional(readOnly = true)
    public TrolleyResponse getByCode(String code) {
        Trolley trolley = trolleyRepository.findByTrolleyCode(code)
                .orElseThrow(() -> new ApiException(ErrorCode.TROLLEY_NOT_FOUND, HttpStatus.NOT_FOUND, "Trolley not found"));
        if (trolley.getStore() != null) {
            authorizationService.requireStoreStaffForStore(trolley.getStore().getId());
        } else {
            authorizationService.requireRole(com.cartvia.cartvia_backend.common.enums.Role.ADMIN);
        }
        return toResponse(trolley);
    }

    @Transactional
    public TrolleyResponse updateStatus(UUID id, UpdateTrolleyStatusRequest request) {
        authorizationService.requireStoreStaffForTrolley(id);
        Trolley trolley = trolleyRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TROLLEY_NOT_FOUND, HttpStatus.NOT_FOUND, "Trolley not found"));
        trolley.setStatus(request.getStatus());
        trolley = trolleyRepository.save(trolley);
        return toResponse(trolley);
    }

    @Transactional
    public void heartbeat(String code, HeartbeatRequest request, String deviceToken) {
        deviceAuthenticationService.requireDeviceForTrolley(deviceToken, code);
        Trolley trolley = trolleyRepository.findByTrolleyCode(code)
                .orElseThrow(() -> new ApiException(ErrorCode.TROLLEY_NOT_FOUND, HttpStatus.NOT_FOUND, "Trolley not found"));

        // A trolley that was INACTIVE (i.e. previously timed out — see
        // TrolleyHeartbeatMonitor — or never seen before) coming back with a
        // heartbeat counts as a fresh connection.
        boolean wasDisconnected = trolley.getStatus() == TrolleyStatus.INACTIVE;

        trolley.setLastSeenAt(LocalDateTime.now());
        if (request.getBatteryPct() != null) {
            trolley.setBatteryPct(request.getBatteryPct());
        }
        if (request.getRssi() != null) {
            trolley.setRssi(request.getRssi());
        }
        if (wasDisconnected) {
            trolley.setStatus(TrolleyStatus.ACTIVE);
        }
        trolleyRepository.save(trolley);

        if (wasDisconnected) {
            webSocketEventPublisher.toTrolley(trolley.getTrolleyCode(), WebSocketEventType.TROLLEY_CONNECTED, toResponse(trolley));
        }
    }

    private TrolleyResponse toResponse(Trolley trolley) {
        return new TrolleyResponse(
                trolley.getId(),
                trolley.getTrolleyCode(),
                trolley.getStore() != null ? trolley.getStore().getId() : null,
                trolley.getStatus(),
                buildQrPayload(trolley),
                trolley.getLastSeenAt());
    }

    private String buildQrPayload(Trolley trolley) {
        return "CARTREX:TROLLEY:" + trolley.getTrolleyCode() + ":" + trolley.getId();
    }
}
