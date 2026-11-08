package com.canteen.unit;

import com.canteen.entity.FoodItem;
import com.canteen.entity.OrderStatus;
import com.canteen.exception.ApiException;
import com.canteen.payment.HmacSigner;
import com.canteen.payment.RazorpayGateway;
import com.canteen.payment.SandboxGateway;
import com.canteen.security.LoginRateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafetyGuardsTest {

    @Test
    @DisplayName("SECURITY: the sandbox gateway refuses to start under the prod profile")
    void sandboxRefusesProd() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> new SandboxGateway("secret", prod))
                .isInstanceOf(IllegalStateException.class);

        MockEnvironment dev = new MockEnvironment();
        dev.setActiveProfiles("dev");
        assertThat(new SandboxGateway("secret", dev).name()).isEqualTo("sandbox");
    }

    @Test
    @DisplayName("The Razorpay gateway refuses to start without its keys")
    void razorpayNeedsKeys() {
        assertThatThrownBy(() -> new RazorpayGateway("", "", ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Razorpay signatures: checkout uses the key secret, webhooks use the webhook secret")
    void razorpaySignatures() {
        RazorpayGateway gateway = new RazorpayGateway("key", "key-secret", "hook-secret");

        String checkout = HmacSigner.signHex("order_1|pay_1", "key-secret");
        assertThat(gateway.isValidCheckoutSignature("order_1", "pay_1", checkout)).isTrue();
        assertThat(gateway.isValidCheckoutSignature("order_2", "pay_1", checkout)).isFalse();
        assertThat(gateway.isValidCheckoutSignature("order_1", "pay_1", null)).isFalse();

        String webhook = HmacSigner.signHex("{\"a\":1}", "hook-secret");
        assertThat(gateway.isValidWebhookSignature("{\"a\":1}", webhook)).isTrue();
        assertThat(gateway.isValidWebhookSignature("{\"a\":2}", webhook)).isFalse();
    }

    @Test
    @DisplayName("Rupees convert to paise exactly")
    void paise() {
        assertThat(RazorpayGateway.toPaise(new BigDecimal("129.50"))).isEqualTo(12950L);
        assertThat(RazorpayGateway.toPaise(new BigDecimal("0.01"))).isEqualTo(1L);
    }

    @Test
    @DisplayName("SECURITY: logins are limited per email, regardless of IP and letter case")
    void rateLimitPerEmail() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, 100);
        limiter.check("a@x.com", "1.1.1.1");
        limiter.check("A@X.COM", "2.2.2.2");
        limiter.check("a@x.com", "3.3.3.3");
        assertThatThrownBy(() -> limiter.check("a@x.com", "4.4.4.4"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        limiter.check("b@x.com", "1.1.1.1");
    }

    @Test
    @DisplayName("SECURITY: logins are limited per IP, across many emails")
    void rateLimitPerIp() {
        LoginRateLimiter limiter = new LoginRateLimiter(100, 2);
        limiter.check("a@x.com", "9.9.9.9");
        limiter.check("b@x.com", "9.9.9.9");
        assertThatThrownBy(() -> limiter.check("c@x.com", "9.9.9.9")).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("Order state machine: allowed paths only, and terminal states stay terminal")
    void stateMachine() {
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.ACCEPTED)).isFalse();
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PLACED)).isTrue();
        assertThat(OrderStatus.READY.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PLACED)).isFalse();
        assertThat(OrderStatus.PREPARING.cancellableByStudent()).isFalse();
        assertThat(OrderStatus.ACCEPTED.cancellableByStudent()).isTrue();
    }

    @Test
    @DisplayName("Effective price uses a special price only when it is a real discount")
    void effectivePrice() {
        FoodItem food = FoodItem.builder().price(new BigDecimal("100")).build();
        assertThat(food.effectivePrice()).isEqualByComparingTo("100");
        food.setSpecialPrice(new BigDecimal("80"));
        assertThat(food.effectivePrice()).isEqualByComparingTo("80");
        food.setSpecialPrice(new BigDecimal("120"));
        assertThat(food.effectivePrice()).isEqualByComparingTo("100");
    }
}
