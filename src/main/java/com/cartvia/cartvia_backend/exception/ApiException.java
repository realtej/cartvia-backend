package com.cartvia.cartvia_backend.exception;

import com.cartvia.cartvia_backend.common.dto.FieldErrorDto;
import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final List<FieldErrorDto> fieldErrors;

    public ApiException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.fieldErrors = Collections.emptyList();
    }

    public ApiException(ErrorCode errorCode, HttpStatus status, String message, List<FieldErrorDto> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}
