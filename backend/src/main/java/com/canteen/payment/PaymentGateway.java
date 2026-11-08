package com.canteen.payment;

import java.math.BigDecimal;

/**
 * What the app needs from a payment provider. Business logic depends only on this,
 * so the sandbox and Razorpay go through exactly the same order and payment code.
 */
public interface PaymentGateway {

    /** Short name stored with each payment and sent to the frontend. */
    String name();

    /** Public key for the browser checkout; null when the gateway has none. */
    String publicKey();

    /** Creates an order at the provider and returns the provider's order id. */
    String createOrder(String receipt, BigDecimal amount);

    boolean isValidCheckoutSignature(String gatewayOrderId, String gatewayPaymentId, String signature);

    boolean isValidWebhookSignature(String rawBody, String signature);

    void refund(String gatewayPaymentId, BigDecimal amount);
}
