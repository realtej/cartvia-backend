package com.cartvia.cartvia_backend.payment;

import com.cartvia.cartvia_backend.order.entity.Order;

/**
 * Abstraction over the payment gateway used to collect customer payment.
 * Swap {@link MockPaymentGatewayService} for a real provider (e.g. Razorpay,
 * PhonePe) integration without touching {@link PaymentService}.
 */
public interface PaymentGatewayService {

    /**
     * Build a UPI deep link the customer's app/browser can open to pay.
     */
    String buildUpiDeepLink(Order order);

    /**
     * Build a scannable UPI QR payload equivalent to the deep link, for
     * customers checking out on a screen without a UPI app context.
     */
    String buildQrPayload(Order order);
}
