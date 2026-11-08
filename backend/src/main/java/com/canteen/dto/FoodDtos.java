package com.canteen.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

public class FoodDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotNull
        private Long categoryId;
        @NotBlank @Size(max = 120)
        private String name;
        @Size(max = 500)
        private String description;
        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal price;
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal specialPrice;
        private Boolean isTodaySpecial;
        @Size(max = 500)
        private String imageUrl;
        @Min(1) @Max(120)
        private Integer prepTimeMin;
        private Boolean isVeg;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateRequest {
        private Long categoryId;
        @Size(max = 120) private String name;
        @Size(max = 500) private String description;
        @DecimalMin(value = "0.0", inclusive = false) private BigDecimal price;
        @DecimalMin(value = "0.0", inclusive = false) private BigDecimal specialPrice;
        /** Removes the discount. A null specialPrice alone means "leave unchanged". */
        private Boolean clearSpecialPrice;
        private Boolean isTodaySpecial;
        @Size(max = 500) private String imageUrl;
        @Min(1) @Max(120) private Integer prepTimeMin;
        private Boolean isVeg;
        private Boolean availability;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal specialPrice;
        /** What the customer pays. Clients show this rather than working it out themselves. */
        private BigDecimal effectivePrice;
        private Boolean isTodaySpecial;
        private Boolean availability;
        private String imageUrl;
        private Integer prepTimeMin;
        private Boolean isVeg;
        private Double averageRating;
        private Long reviewCount;
    }
}