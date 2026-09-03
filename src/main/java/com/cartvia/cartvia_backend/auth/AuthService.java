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
import com.cartvia.cartvia_backend.auth.dto.UserSummaryDto;
import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.config.CartviaProperties;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.security.JwtService;
import com.cartvia.cartvia_backend.security.TokenHashService;
import com.cartvia.cartvia_backend.user.entity.PasswordResetToken;
import com.cartvia.cartvia_backend.user.entity.RefreshToken;
import com.cartvia.cartvia_backend.user.entity.User;
import com.cartvia.cartvia_backend.user.repository.PasswordResetTokenRepository;
import com.cartvia.cartvia_backend.user.repository.RefreshTokenRepository;
import com.cartvia.cartvia_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;
    private final CartviaProperties cartviaProperties;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(ErrorCode.USERNAME_TAKEN, HttpStatus.CONFLICT,
                    "Username '" + request.getUsername() + "' is already taken");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN, HttpStatus.CONFLICT, "Email is already registered");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new ApiException(ErrorCode.PHONE_TAKEN, HttpStatus.CONFLICT, "Phone is already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(blankToNull(request.getEmail()));
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setGender(blankToNull(request.getGender()));
        user.setRole(Role.CUSTOMER);
        user = userRepository.save(user);

        return new RegisterResponse(user.getId());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                    "Invalid username or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = issueRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken, toUserSummary(user));
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String hash = tokenHashService.hashToken(request.getRefreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED,
                        "Invalid or expired refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            throw new ApiException(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token");
        }

        User user = refreshToken.getUser();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        return new TokenResponse(accessToken);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();
        boolean hasPhone = request.getPhone() != null && !request.getPhone().isBlank();
        if (!hasEmail && !hasPhone) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Email or phone is required");
        }

        User user = null;
        if (hasEmail) {
            user = userRepository.findByEmail(request.getEmail()).orElse(null);
        }
        if (user == null && hasPhone) {
            user = userRepository.findByPhone(request.getPhone()).orElse(null);
        }

        if (user != null) {
            String rawToken = tokenHashService.generateOpaqueToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(tokenHashService.hashToken(rawToken));
            resetToken.setExpiresAt(LocalDateTime.now().plus(
                    cartviaProperties.getJwt().getResetTokenExpirationMinutes(), ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(resetToken);
            log.info("Password reset token issued for user {} (dev only log): {}", user.getUsername(), rawToken);
        }

        return new ForgotPasswordResponse(true);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        String hash = tokenHashService.hashToken(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(hash)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED,
                        "Invalid or expired reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED,
                    "Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        resetToken.setUsed(true);
        return new ResetPasswordResponse(true);
    }

    private String issueRefreshToken(User user) {
        String rawToken = tokenHashService.generateOpaqueToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHashService.hashToken(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plus(
                cartviaProperties.getJwt().getRefreshTokenExpirationDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private UserSummaryDto toUserSummary(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
