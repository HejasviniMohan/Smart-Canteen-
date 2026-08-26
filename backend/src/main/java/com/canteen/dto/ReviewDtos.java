package com.canteen.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class ReviewDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotNull
        private Long orderId;
        @NotNull
        private Long foodItemId;
        @NotNull @Min(1) @Max(5)
        private Integer rating;
        @Size(max = 500)
        private String comment;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private String userName;
        private Long foodItemId;
        private String foodItemName;
        private Long orderId;
        private Integer rating;
        private String comment;
        private LocalDateTime createdAt;
    }
}