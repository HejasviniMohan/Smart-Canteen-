package com.canteen.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal specialPrice;

    @Column(name = "is_today_special")
    @Builder.Default
    private Boolean isTodaySpecial = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean availability = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "prep_time_min", nullable = false)
    @Builder.Default
    private Integer prepTimeMin = 10;

    @Column(name = "is_veg", nullable = false)
    @Builder.Default
    private Boolean isVeg = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.availability == null) this.availability = true;
        if (this.isVeg == null) this.isVeg = true;
        if (this.prepTimeMin == null) this.prepTimeMin = 10;
        if (this.isTodaySpecial == null) this.isTodaySpecial = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}