package com.canteen.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

public class CategoryDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotBlank @Size(max = 80)
        private String name;
        @Size(max = 255)
        private String description;
        @Size(max = 500)
        private String imageUrl;
        private Integer displayOrder;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateRequest {
        @Size(max = 80)
        private String name;
        @Size(max = 255)
        private String description;
        @Size(max = 500)
        private String imageUrl;
        private Integer displayOrder;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String imageUrl;
        private Integer displayOrder;
    }
}