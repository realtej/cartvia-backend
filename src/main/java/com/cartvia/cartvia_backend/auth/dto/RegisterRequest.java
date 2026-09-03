package com.cartvia.cartvia_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 15)
    private String phone;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @Size(max = 10)
    private String gender;
}
