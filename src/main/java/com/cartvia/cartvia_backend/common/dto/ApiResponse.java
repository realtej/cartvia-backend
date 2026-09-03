package com.cartvia.cartvia_backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String errorCode,
        String message,
        List<FieldErrorDto> fieldErrors) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    public static <T> ApiResponse<T> successResponse() {
        return success(null);
    }

    public static <T> ApiResponse<T> successResponse(T data) {
        return success(data);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode, message, null);
    }

    public static <T> ApiResponse<T> validationError(String message, List<FieldErrorDto> fieldErrors) {
        return new ApiResponse<>(false, null, "VALIDATION_ERROR", message, fieldErrors);
    }
}
