package com.cartvia.cartvia_backend.common.dto;

public record FieldErrorDto(
        String field,
        String constraint,
        Object rejectedValue,
        String message) {
}
