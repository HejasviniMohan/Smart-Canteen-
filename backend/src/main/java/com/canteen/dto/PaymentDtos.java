package com.canteen.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

public class PaymentDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateOrderRequest {
        @NotNull
        private Long orderId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RazorpayOrderResponse {
        private Long paymentId;
        private String razorpayOrderId;
        private BigDecimal amount;
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
    public static class PaymentResponse {
        private Long id;
        private Long orderId;
        private String tokenNumber;
        private BigDecimal amount;
        private String paymentMethod;
        private String paymentStatus;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String transactionId;
    }
}