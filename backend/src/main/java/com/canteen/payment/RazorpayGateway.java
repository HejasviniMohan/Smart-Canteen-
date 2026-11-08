package com.canteen.payment;

import com.canteen.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/** Razorpay's REST API. Card and UPI details stay on Razorpay's hosted checkout. */
@Component
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "razorpay")
@Slf4j
public class RazorpayGateway implements PaymentGateway {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RestClient client;

    public RazorpayGateway(@Value("${app.razorpay.key-id}") String keyId,
                           @Value("${app.razorpay.key-secret}") String keySecret,
                           @Value("${app.razorpay.webhook-secret}") String webhookSecret) {
        if (keyId.isBlank() || keySecret.isBlank() || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                    "RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET and RAZORPAY_WEBHOOK_SECRET are required");
        }
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;

        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(5));
        timeouts.setReadTimeout(Duration.ofSeconds(15));
        this.client = RestClient.builder()
                .baseUrl("https://api.razorpay.com/v1")
                .requestFactory(timeouts)
                .defaultHeaders(h -> h.setBasicAuth(keyId, keySecret))
                .build();
    }

    @Override
    public String name() { return "razorpay"; }

    @Override
    public String publicKey() { return keyId; }

    @Override
    @SuppressWarnings("unchecked")
    public String createOrder(String receipt, BigDecimal amount) {
        try {
            Map<String, Object> response = client.post().uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "amount", toPaise(amount),
                            "currency", "INR",
                            "receipt", receipt))
                    .retrieve()
                    .body(Map.class);
            return (String) response.get("id");
        } catch (RestClientException e) {
            log.error("Razorpay order creation failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment provider is unavailable. Please try again.");
        }
    }

    @Override
    public boolean isValidCheckoutSignature(String gatewayOrderId, String gatewayPaymentId, String signature) {
        return HmacSigner.matchesHex(gatewayOrderId + "|" + gatewayPaymentId, keySecret, signature);
    }

    @Override
    public boolean isValidWebhookSignature(String rawBody, String signature) {
        return HmacSigner.matchesHex(rawBody, webhookSecret, signature);
    }

    @Override
    public void refund(String gatewayPaymentId, BigDecimal amount) {
        try {
            client.post().uri("/payments/{id}/refund", gatewayPaymentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", toPaise(amount)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Razorpay refund failed for {}", gatewayPaymentId, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Refund could not be started. Please try again.");
        }
    }

    public static long toPaise(BigDecimal rupees) {
        return rupees.movePointRight(2).longValueExact();
    }
}
