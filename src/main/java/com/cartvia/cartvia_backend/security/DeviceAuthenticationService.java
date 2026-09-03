package com.cartvia.cartvia_backend.security;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.trolley.entity.Trolley;
import com.cartvia.cartvia_backend.trolley.repository.TrolleyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceAuthenticationService {

    private final TrolleyRepository trolleyRepository;
    private final TokenHashService tokenHashService;

    public Optional<DevicePrincipal> authenticate(String rawDeviceToken, String trolleyCode) {
        if (rawDeviceToken == null || rawDeviceToken.isBlank()) {
            return Optional.empty();
        }
        String hash = tokenHashService.hashToken(rawDeviceToken);
        return trolleyRepository.findByDeviceTokenHash(hash)
                .filter(trolley -> trolley.getTrolleyCode().equals(trolleyCode))
                .map(trolley -> new DevicePrincipal(trolley.getId(), trolley.getTrolleyCode()));
    }

    public Optional<DevicePrincipal> authenticateByToken(String rawDeviceToken) {
        if (rawDeviceToken == null || rawDeviceToken.isBlank()) {
            return Optional.empty();
        }
        String hash = tokenHashService.hashToken(rawDeviceToken);
        return trolleyRepository.findByDeviceTokenHash(hash)
                .map(trolley -> new DevicePrincipal(trolley.getId(), trolley.getTrolleyCode()));
    }

    public DevicePrincipal requireDeviceForTrolley(String rawDeviceToken, String trolleyCode) {
        return authenticate(rawDeviceToken, trolleyCode)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.INVALID_TOKEN,
                        HttpStatus.UNAUTHORIZED,
                        "Device token does not match trolley code"));
    }

    public Trolley requireTrolleyForDevice(DevicePrincipal principal, String trolleyCode) {
        Trolley trolley = trolleyRepository.findByTrolleyCode(trolleyCode)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.TROLLEY_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Trolley not found"));
        if (!trolley.getId().equals(principal.getTrolleyId())) {
            throw new ApiException(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED,
                    "Device token does not match trolley code");
        }
        return trolley;
    }
}
