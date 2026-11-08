package com.canteen.api;

import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentMethod;
import com.canteen.entity.PaymentStatus;
import com.canteen.payment.HmacSigner;
import com.canteen.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentApiTest extends IntegrationTest {

    @Value("${app.payment.sandbox-secret}")
    private String webhookSecret;

    private record Started(Actor student, long orderId, long paymentId, String gatewayOrderId) {}

    private Started startPayment() throws Exception {
        Actor student = student();
        long orderId = placeOrder(student, food("50.00", null), PaymentMethod.ONLINE).get("id").asLong();
        JsonNode checkout = read(send(post("/api/payments/create-order"), student, Map.of("orderId", orderId))
                .andExpect(status().isCreated()));
        return new Started(student, orderId, checkout.get("paymentId").asLong(),
                checkout.get("razorpayOrderId").asText());
    }

    private String webhookBody(String event, String gatewayOrderId, long paise) {
        return """
                {"event":"%s","payload":{"payment":{"entity":{
                  "id":"pay_%s","order_id":"%s","amount":%d,"method":"upi"}}}}
                """.formatted(event, UUID.randomUUID(), gatewayOrderId, paise);
    }

    private ResultActions postWebhook(String body, String signature, String eventId) throws Exception {
        return mvc.perform(post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", signature)
                .header("X-Razorpay-Event-Id", eventId)
                .content(body));
    }

    private ResultActions signedWebhook(String body) throws Exception {
        return postWebhook(body, HmacSigner.signHex(body, webhookSecret), UUID.randomUUID().toString());
    }

    private PaymentStatus paymentStatusOf(long orderId) {
        return orders.findById(orderId).orElseThrow().getPaymentStatus();
    }

    @Test
    @DisplayName("SECURITY: the old hardcoded 'demo-signature' no longer pays for anything")
    void demoSignatureRejected() throws Exception {
        Started s = startPayment();
        send(post("/api/payments/verify"), s.student(), Map.of(
                "paymentId", s.paymentId(),
                "razorpayOrderId", s.gatewayOrderId(),
                "razorpayPaymentId", "pay_DEMO_1",
                "razorpaySignature", "demo-signature"))
                .andExpect(status().isBadRequest());

        assertThat(paymentStatusOf(s.orderId())).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("SECURITY: a student cannot start, verify or read payment for someone else's order")
    void paymentOwnership() throws Exception {
        Started s = startPayment();
        Actor other = student();

        send(post("/api/payments/create-order"), other, Map.of("orderId", s.orderId()))
                .andExpect(status().isForbidden());
        send(post("/api/payments/verify"), other, Map.of(
                "paymentId", s.paymentId(),
                "razorpayOrderId", s.gatewayOrderId(),
                "razorpayPaymentId", "x",
                "razorpaySignature", "y"))
                .andExpect(status().isForbidden());
        send(post("/api/payments/sandbox/pay"), other, Map.of("paymentId", s.paymentId(), "success", true))
                .andExpect(status().isForbidden());
        send(get("/api/payments/" + s.paymentId()), other, null).andExpect(status().isForbidden());
        send(get("/api/payments/" + s.paymentId()), s.student(), null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A failed attempt can be retried on the same order without a duplicate payment")
    void retryAfterFailure() throws Exception {
        Started s = startPayment();
        send(post("/api/payments/sandbox/pay"), s.student(), Map.of("paymentId", s.paymentId(), "success", false))
                .andExpect(status().isPaymentRequired());
        assertThat(payments.findById(s.paymentId()).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

        payOnline(s.student(), s.orderId());

        assertThat(paymentStatusOf(s.orderId())).isEqualTo(PaymentStatus.PAID);
        assertThat(payments.findByOrderId(s.orderId()).orElseThrow().getId()).isEqualTo(s.paymentId());
    }

    @Test
    @DisplayName("Cash orders cannot go through online payment")
    void cashOrderCannotPayOnline() throws Exception {
        Actor student = student();
        long id = placeOrder(student, food("50.00", null), PaymentMethod.CASH_ON_PICKUP).get("id").asLong();
        send(post("/api/payments/create-order"), student, Map.of("orderId", id)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("WEBHOOK: pays the order even though the browser never called back")
    void webhookPaysOrder() throws Exception {
        Started s = startPayment();
        signedWebhook(webhookBody("payment.captured", s.gatewayOrderId(), 5000)).andExpect(status().isOk());

        assertThat(paymentStatusOf(s.orderId())).isEqualTo(PaymentStatus.PAID);
        assertThat(orders.findById(s.orderId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(payments.findById(s.paymentId()).orElseThrow().getPaymentMethod()).isEqualTo("UPI");
    }

    @Test
    @DisplayName("WEBHOOK SECURITY: a bad signature is rejected and changes nothing")
    void webhookBadSignature() throws Exception {
        Started s = startPayment();
        String body = webhookBody("payment.captured", s.gatewayOrderId(), 5000);

        postWebhook(body, "forged", UUID.randomUUID().toString()).andExpect(status().isBadRequest());
        postWebhook(body, HmacSigner.signHex(body, "wrong-secret"), UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());

        assertThat(paymentStatusOf(s.orderId())).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("WEBHOOK: a signed event with the wrong amount doesn't settle the order")
    void webhookWrongAmount() throws Exception {
        Started s = startPayment();
        signedWebhook(webhookBody("payment.captured", s.gatewayOrderId(), 100)).andExpect(status().isOk());
        assertThat(paymentStatusOf(s.orderId())).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("WEBHOOK: a redelivered event is processed once")
    void webhookDeduplicated() throws Exception {
        Started s = startPayment();
        String failed = webhookBody("payment.failed", s.gatewayOrderId(), 5000);
        String sig = HmacSigner.signHex(failed, webhookSecret);
        String eventId = UUID.randomUUID().toString();

        postWebhook(failed, sig, eventId).andExpect(status().isOk());
        assertThat(payments.findById(s.paymentId()).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

        // A retry resets the payment. A replay of the old failure must not undo that.
        send(post("/api/payments/create-order"), s.student(), Map.of("orderId", s.orderId()))
                .andExpect(status().isCreated());
        postWebhook(failed, sig, eventId).andExpect(status().isOk());

        assertThat(payments.findById(s.paymentId()).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Browser callback and webhook both arriving settles the order only once")
    void verifyThenWebhookIsSafe() throws Exception {
        Started s = startPayment();
        payOnline(s.student(), s.orderId());
        int notificationsBefore = notificationCount(s.student());

        signedWebhook(webhookBody("payment.captured", s.gatewayOrderId(), 5000)).andExpect(status().isOk());

        assertThat(paymentStatusOf(s.orderId())).isEqualTo(PaymentStatus.PAID);
        assertThat(notificationCount(s.student())).isEqualTo(notificationsBefore);
    }

    @Test
    @DisplayName("Cancelling a paid order refunds it")
    void cancelRefunds() throws Exception {
        Started s = startPayment();
        payOnline(s.student(), s.orderId());

        send(delete("/api/orders/" + s.orderId()), s.student(), null).andExpect(status().isNoContent());

        assertThat(paymentStatusOf(s.orderId())).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payments.findById(s.paymentId()).orElseThrow().getRefundedAt()).isNotNull();
    }

    @Test
    @DisplayName("Money arriving after an order was cancelled is refunded, and the order stays cancelled")
    void lateMoneyIsRefunded() throws Exception {
        Started s = startPayment();
        send(delete("/api/orders/" + s.orderId()), s.student(), null).andExpect(status().isNoContent());

        signedWebhook(webhookBody("payment.captured", s.gatewayOrderId(), 5000)).andExpect(status().isOk());

        assertThat(orders.findById(s.orderId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(paymentStatusOf(s.orderId())).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("Admins can filter the payments list")
    void adminPaymentsList() throws Exception {
        Started s = startPayment();
        payOnline(s.student(), s.orderId());
        send(get("/api/admin/payments?status=PAID"), admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].paymentStatus").value("PAID"));
    }

    private int notificationCount(Actor who) throws Exception {
        return read(send(get("/api/notifications/me"), who, null)).size();
    }
}
