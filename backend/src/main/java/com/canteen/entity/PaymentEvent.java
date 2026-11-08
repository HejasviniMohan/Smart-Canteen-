package com.canteen.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentEvent {

    @Id
    @Column(name = "event_id", length = 120)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
