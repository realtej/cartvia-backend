package com.cartvia.cartvia_backend.admin.dto;

import java.util.List;
import java.util.UUID;

public record BulkDeactivateResponse(int requested, int succeeded, List<UUID> failedIds) {
}
