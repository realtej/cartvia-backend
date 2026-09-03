package com.cartvia.cartvia_backend.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BulkDeactivateRequest {

    @NotEmpty
    private List<UUID> productIds;
}
