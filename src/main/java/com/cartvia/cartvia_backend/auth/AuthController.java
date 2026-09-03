package com.cartvia.cartvia_backend.auth;

import com.cartvia.cartvia_backend.auth.dto.ForgotPasswordRequest;
import com.cartvia.cartvia_backend.auth.dto.ForgotPasswordResponse;
import com.cartvia.cartvia_backend.auth.dto.LoginRequest;
import com.cartvia.cartvia_backend.auth.dto.LoginResponse;
import com.cartvia.cartvia_backend.auth.dto.RefreshRequest;
import com.cartvia.cartvia_backend.auth.dto.RegisterRequest;
import com.cartvia.cartvia_backend.auth.dto.RegisterResponse;
import com.cartvia.cartvia_backend.auth.dto.ResetPasswordRequest;
import com.cartvia.cartvia_backend.auth.dto.ResetPasswordResponse;
import com.cartvia.cartvia_backend.auth.dto.TokenResponse;
import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPassword(request)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.resetPassword(request)));
    }
}
