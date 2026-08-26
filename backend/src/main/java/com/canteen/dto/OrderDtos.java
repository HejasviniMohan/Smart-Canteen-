package com.canteen.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentStatus;

public class OrderDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlaceOrderRequest {
        @NotEmpty @Valid
        private List<OrderItemRequest> items;
        @Size(max = 500)
        private String notes;
        private String paymentMethod;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderItemRequest {
        @NotNull
        private Long foodItemId;
        @NotNull @Min(1) @Max(50)
        private Integer quantity;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatusUpdateRequest {
        @NotNull
        private OrderStatus status;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long foodItemId;
        private String foodItemName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderResponse {
        private Long id;
        private Long userId;
        private String userName;
        private String userEmail;
        private BigDecimal totalAmount;
        private OrderStatus status;
        private PaymentStatus paymentStatus;
        private String tokenNumber;
        private String notes;
        private LocalDateTime orderDate;
        private LocalDateTime updatedAt;
        private List<OrderItemResponse> items;
    }
}