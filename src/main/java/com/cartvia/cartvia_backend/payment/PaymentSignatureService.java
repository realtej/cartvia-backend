package com.cartvia.cartvia_backend.payment;

import com.cartvia.cartvia_backend.config.CartviaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Verifies inbound payment webhook signatures using HMAC-SHA256 over a
 * canonical representation of the event, keyed with
 * {@code cartvia.payment.webhook-secret} (see {@code PAYMENT_WEBHOOK_SECRET}
 * in .env.example). This is what stands between "any POST body with
 * {status: SUCCESS}" and an actual gateway-issued event (spec §11.1).
 * <p>
 * Accepts either a bare hex digest, or the Razorpay-style
 * {@code t=<timestamp>,v1=<hexHmac>} format — only the {@code v1} segment is
 * checked, matching how real gateways structure their signature header.
 */
@Component
@RequiredArgsConstructor
public class PaymentSignatureService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final CartviaProperties cartviaProperties;

    public boolean isValid(UUID orderId, String status, String upiTxnRef, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String candidateHex = extractHexDigest(signature);
        if (candidateHex == null) {
            return false;
        }
        String expectedHex = computeHmac(canonicalPayload(orderId, status, upiTxnRef));
        return constantTimeEquals(expectedHex, candidateHex);
    }

    private String canonicalPayload(UUID orderId, String status, String upiTxnRef) {
        return orderId + "|" + status + "|" + (upiTxnRef == null ? "" : upiTxnRef);
    }

    private String extractHexDigest(String signature) {
        int v1Index = signature.indexOf("v1=");
        String raw = v1Index >= 0 ? signature.substring(v1Index + 3) : signature;
        int comma = raw.indexOf(',');
        if (comma >= 0) {
            raw = raw.substring(0, comma);
        }
        raw = raw.trim();
        return isHex(raw) ? raw : null;
    }

    private boolean isHex(String value) {
        if (value.isEmpty() || value.length() % 2 != 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private String computeHmac(String payload) {
        try {
            String secret = cartviaProperties.getPayment().getWebhookSecret();
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to compute webhook signature", ex);
        }
    }

    private boolean constantTimeEquals(String expectedHex, String candidateHex) {
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.UTF_8),
                candidateHex.getBytes(StandardCharsets.UTF_8));
    }
}
