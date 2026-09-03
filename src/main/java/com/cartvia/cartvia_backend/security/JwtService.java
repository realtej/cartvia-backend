package com.cartvia.cartvia_backend.security;

import com.cartvia.cartvia_backend.config.CartviaProperties;
import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final CartviaProperties properties;
    private final SecretKey secretKey;

    public JwtService(CartviaProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getJwt().getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED, "Access token has expired");
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public Role extractRole(String token) {
        String role = parseClaims(token).get("role", String.class);
        return Role.valueOf(role);
    }
}
