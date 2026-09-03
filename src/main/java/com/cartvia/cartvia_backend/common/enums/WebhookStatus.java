package com.cartvia.cartvia_backend.common.enums;

/**
 * Status values accepted on an inbound payment gateway webhook.
 * Distinct from {@link PaymentStatus}: a webhook can only ever report a
 * terminal outcome (SUCCESS or FAILED), never INITIATED.
 */
public enum WebhookStatus {
    SUCCESS,
    FAILED
}
