package com.cartvia.cartvia_backend.exception;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.common.dto.FieldErrorDto;
import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ApiResponse<Void> body;
        if (ex.getFieldErrors().isEmpty()) {
            body = ApiResponse.error(ex.getErrorCode().name(), ex.getMessage());
        } else {
            body = ApiResponse.validationError(ex.getMessage(), ex.getFieldErrors());
        }
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Validation failed", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // Covers malformed JSON and invalid enum values submitted in a request
        // body (e.g. a bad WebhookStatus/OrderStatus/Role value) — must be a
        // 400 VALIDATION_ERROR, not a silent default or 500.
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), "Malformed or invalid request body"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDto> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDto(
                        v.getPropertyPath().toString(),
                        "invalid",
                        v.getInvalidValue(),
                        v.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Validation failed", fieldErrors));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        String message = ex instanceof BadCredentialsException
                ? "Invalid username or password"
                : "Authentication is required";
        ErrorCode code = ex instanceof BadCredentialsException
                ? ErrorCode.INVALID_CREDENTIALS
                : ErrorCode.INVALID_TOKEN;
        HttpStatus status = ex instanceof BadCredentialsException ? HttpStatus.UNAUTHORIZED : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status)
                .body(ApiResponse.error(code.name(), message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.name(),
                        "You do not have permission to perform this operation"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), "An unexpected error occurred"));
    }

    private FieldErrorDto toFieldError(FieldError fieldError) {
        String constraint = fieldError.getCode();
        Object rejected = fieldError.getRejectedValue();
        if ("password".equals(fieldError.getField())) {
            rejected = null;
        }
        return new FieldErrorDto(
                fieldError.getField(),
                constraint,
                rejected,
                fieldError.getDefaultMessage());
    }
}
