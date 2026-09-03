package com.cartvia.cartvia_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 15)
    private String phone;
}
