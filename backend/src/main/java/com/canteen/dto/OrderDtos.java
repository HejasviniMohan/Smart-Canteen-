package com.canteen.dto;

import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentMethod;
import com.canteen.entity.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlaceOrderRequest {
        @NotEmpty @Size(max = 50) @Valid
        private List<OrderItemRequest> items;
        @Size(max = 500)
        private String notes;
        @NotNull
        private PaymentMethod paymentMethod;
        @NotNull
        private LocalDateTime pickupSlot;
        /** The total the student was shown. If prices have changed the order is refused. */
        private BigDecimal expectedTotal;
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
        private PaymentMethod paymentMethod;
        private String tokenNumber;
        private LocalDateTime pickupSlot;
        /** Signed QR payload. Only ever sent to the order's owner. */
        private String pickupCode;
        private String notes;
        private LocalDateTime orderDate;
        private LocalDateTime updatedAt;
        private LocalDateTime collectedAt;
        private LocalDateTime paymentDeadline;
        private List<OrderItemResponse> items;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PickupLookupRequest {
        /** Scanned QR payload. */
        @Size(max = 300)
        private String code;
        /** Manual fallback when the camera cannot be used. */
        @Size(max = 20)
        private String token;
    }
}
