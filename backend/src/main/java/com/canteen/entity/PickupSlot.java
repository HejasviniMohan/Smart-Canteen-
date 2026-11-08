package com.canteen.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pickup_slots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PickupSlot {

    @Id
    @Column(name = "slot_start")
    private LocalDateTime slotStart;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer booked;
}
