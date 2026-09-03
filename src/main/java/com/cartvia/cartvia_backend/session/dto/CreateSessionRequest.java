package com.cartvia.cartvia_backend.session.dto;

import com.cartvia.cartvia_backend.common.enums.SessionStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateSessionRequest {

    @NotBlank
    private String trolleyCode;

    private UUID userId;
}
