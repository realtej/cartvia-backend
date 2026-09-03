package com.cartvia.cartvia_backend.payment;

import com.cartvia.cartvia_backend.config.CartviaProperties;
import com.cartvia.cartvia_backend.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * MOCK / NON-PRODUCTION payment gateway implementation.
 * <p>
 * Generates a syntactically valid UPI deep link and a mock QR payload
 * locally, with no external gateway call, network dependency, or real
 * money movement. Enabled by default via {@code cartvia.payment.mock-enabled}
 * (see {@code PAYMENT_MOCK_ENABLED} in .env.example). Replace this bean with
 * a real gateway integration (Razorpay/PhonePe/etc.) before going to
 * production — {@link PaymentGatewayService} is the seam to implement against.
 */
@Service
@RequiredArgsConstructor
public class MockPaymentGatewayService implements PaymentGatewayService {

    private static final String CURRENCY = "INR";

    private final CartviaProperties cartviaProperties;

    @Override
    public String buildUpiDeepLink(Order order) {
        CartviaProperties.Payment payment = cartviaProperties.getPayment();
        String amount = order.getGrandTotal().toPlainString();
        return "upi://pay?pa=" + encode(payment.getUpiPayeeAddress())
                + "&pn=" + encode(payment.getUpiPayeeName())
                + "&am=" + encode(amount)
                + "&tr=" + encode(order.getOrderCode())
                + "&cu=" + CURRENCY;
    }

    @Override
    public String buildQrPayload(Order order) {
        // Illustrative mock QR payload (not a real EMVCo-compliant string).
        // Non-production: for real UPI QR generation, a proper EMVCo/NPCI
        // encoder must be used instead.
        CartviaProperties.Payment payment = cartviaProperties.getPayment();
        return "MOCKQR|pa=" + payment.getUpiPayeeAddress()
                + "|pn=" + payment.getUpiPayeeName()
                + "|am=" + order.getGrandTotal().toPlainString()
                + "|tr=" + order.getOrderCode()
                + "|cu=" + CURRENCY;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
