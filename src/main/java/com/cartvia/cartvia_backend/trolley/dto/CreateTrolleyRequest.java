package com.cartvia.cartvia_backend.trolley.dto;

import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateTrolleyRequest {

    @NotNull
    private String trolleyCode;

    private UUID storeId;

    @Pattern(regexp = "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
    private String esp32Mac;
}
