package com.canteen.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateOrderRequest {
        @NotNull
        private Long orderId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CheckoutResponse {
        private Long paymentId;
        /** Which checkout the frontend opens: "razorpay" or "sandbox". */
        private String gateway;
        private String razorpayOrderId;
        private BigDecimal amount;
        private long amountInPaise;
        private String currency;
        private String keyId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VerifyPaymentRequest {
        @NotNull
        private Long paymentId;
        @NotBlank
        private String razorpayOrderId;
        @NotBlank
        private String razorpayPaymentId;
        @NotBlank
        private String razorpaySignature;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SandboxPayRequest {
        @NotNull
        private Long paymentId;
        @NotNull
        private Boolean success;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SandboxPayResponse {
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentResponse {
        private Long id;
        private Long orderId;
        private String tokenNumber;
        private BigDecimal amount;
        private String gateway;
        private String paymentMethod;
        private String paymentStatus;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String transactionId;
        private LocalDateTime paymentDate;
        private LocalDateTime refundedAt;
        private LocalDateTime createdAt;
    }
}
