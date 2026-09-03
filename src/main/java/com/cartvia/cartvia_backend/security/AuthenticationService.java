package com.cartvia.cartvia_backend.security;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.user.entity.User;
import com.cartvia.cartvia_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    public UUID getAuthenticatedUserId() {
        return getUserPrincipal().getId();
    }

    public Role getAuthenticatedRole() {
        return getUserPrincipal().getRole();
    }

    public UUID getAuthenticatedStoreId() {
        UUID storeId = getUserPrincipal().getStoreId();
        if (storeId == null) {
            throw new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                    "You do not have permission to perform this operation");
        }
        return storeId;
    }

    public User getAuthenticatedUser() {
        return userRepository.findById(getAuthenticatedUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserPrincipal getUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return principal;
    }

    public DevicePrincipal getDevicePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof DevicePrincipal principal)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return principal;
    }

    public boolean isDeviceAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof DevicePrincipal;
    }
}
