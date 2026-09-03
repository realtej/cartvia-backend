package com.cartvia.cartvia_backend.trolley.dto;

import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTrolleyStatusRequest {

    @NotNull
    private TrolleyStatus status;
}
