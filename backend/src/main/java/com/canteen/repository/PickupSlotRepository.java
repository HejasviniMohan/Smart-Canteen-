package com.canteen.repository;

import com.canteen.entity.PickupSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PickupSlotRepository extends JpaRepository<PickupSlot, LocalDateTime> {

    List<PickupSlot> findBySlotStartBetween(LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO pickup_slots (slot_start, capacity, booked)
            VALUES (:slotStart, :capacity, 0)
            ON CONFLICT (slot_start) DO NOTHING
            """)
    void createIfMissing(LocalDateTime slotStart, int capacity);

    /**
     * Returns 1 if a place was taken, 0 if the slot is full. Check and increment happen
     * in one statement, so concurrent requests cannot both take the last place.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE pickup_slots SET booked = booked + 1
             WHERE slot_start = :slotStart AND booked < capacity
            """)
    int tryBook(LocalDateTime slotStart);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE pickup_slots SET booked = booked - 1
             WHERE slot_start = :slotStart AND booked > 0
            """)
    int release(LocalDateTime slotStart);
}
