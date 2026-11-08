package com.canteen.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulated provider so the project can be demoed without any account. It still signs and
 * verifies with real HMACs, so all the verification code runs. It moves no money, which is
 * why it refuses to start under the prod profile.
 */
@Component
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "sandbox")
public class SandboxGateway implements PaymentGateway {

    private final String secret;

    public SandboxGateway(@Value("${app.payment.sandbox-secret}") String secret, Environment env) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("The sandbox payment gateway must not run under the prod profile");
        }
        this.secret = secret;
    }

    @Override
    public String name() { return "sandbox"; }

    @Override
    public String publicKey() { return null; }

    @Override
    public String createOrder(String receipt, BigDecimal amount) {
        return "sandbox_order_" + UUID.randomUUID();
    }

    @Override
    public boolean isValidCheckoutSignature(String gatewayOrderId, String gatewayPaymentId, String signature) {
        return HmacSigner.matchesHex(gatewayOrderId + "|" + gatewayPaymentId, secret, signature);
    }

    @Override
    public boolean isValidWebhookSignature(String rawBody, String signature) {
        return HmacSigner.matchesHex(rawBody, secret, signature);
    }

    @Override
    public void refund(String gatewayPaymentId, BigDecimal amount) {
        // Nothing to do: no money ever moved.
    }

    /** Plays the provider's part: signs a completed payment as Razorpay would. */
    public String signCheckout(String gatewayOrderId, String gatewayPaymentId) {
        return HmacSigner.signHex(gatewayOrderId + "|" + gatewayPaymentId, secret);
    }
}
