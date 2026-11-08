package com.canteen.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** Short human-readable number for the counter. Unique per business date only. */
    @Column(name = "token_number", nullable = false, length = 20)
    private String tokenNumber;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "pickup_slot", nullable = false)
    private LocalDateTime pickupSlot;

    /** Random value signed into the QR code, so codes cannot be derived from the order id. */
    @Column(name = "pickup_nonce", nullable = false, length = 40)
    private String pickupNonce;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "collected_by", length = 150)
    private String collectedBy;

    @Version
    private Long version;

    @Column(length = 500)
    private String notes;

    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.orderDate == null) this.orderDate = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.status == null) this.status = OrderStatus.PLACED;
        if (this.paymentStatus == null) this.paymentStatus = PaymentStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}