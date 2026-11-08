package com.canteen.slots;

import com.canteen.entity.OrderStatus;
import com.canteen.entity.PickupSlot;
import com.canteen.exception.ApiException;
import com.canteen.repository.OrderRepository;
import com.canteen.repository.PickupSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pickup slots cap how many orders the kitchen takes per time window. A queue forms when
 * more people arrive than the kitchen can serve, so this addresses its cause.
 */
@Service
@RequiredArgsConstructor
public class SlotService {

    private static final int MAX_SLOTS_LISTED = 24;
    private static final int MAX_DAYS_AHEAD = 2;

    public record SlotView(LocalDateTime start, int capacity, int remaining) {}

    public record CanteenStatus(boolean open, String opensAt, String closesAt, long ordersInKitchen,
                                long ordersReady, int estimatedWaitMinutes, LocalDateTime nextSlot) {}

    private final PickupSlotRepository slotRepo;
    private final OrderRepository orderRepo;
    private final CanteenProperties props;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<SlotView> upcoming() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime first = roundUp(now.plusMinutes(props.getMinLeadMinutes()));
        LocalDateTime horizon = now.plusHours(24);

        Map<LocalDateTime, PickupSlot> existing = slotRepo.findBySlotStartBetween(first, horizon).stream()
                .collect(Collectors.toMap(PickupSlot::getSlotStart, Function.identity()));

        List<SlotView> slots = new ArrayList<>();
        for (LocalDateTime t = first; t.isBefore(horizon) && slots.size() < MAX_SLOTS_LISTED;
             t = t.plusMinutes(props.getSlotMinutes())) {
            if (!isOpenAt(t)) continue;
            PickupSlot row = existing.get(t);
            int capacity = row == null ? props.getSlotCapacity() : row.getCapacity();
            int booked = row == null ? 0 : row.getBooked();
            slots.add(new SlotView(t, capacity, Math.max(0, capacity - booked)));
        }
        return slots;
    }

    /** Takes one place in a slot, or fails with 409. Rolls back with the surrounding order. */
    @Transactional
    public void book(LocalDateTime slot) {
        LocalDateTime now = LocalDateTime.now(clock);
        boolean aligned = slot.getSecond() == 0 && slot.getNano() == 0
                && slot.getMinute() % props.getSlotMinutes() == 0;
        if (!aligned || !isOpenAt(slot) || !slot.isAfter(now) || slot.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That pickup time is not available");
        }
        slotRepo.createIfMissing(slot, props.getSlotCapacity());
        if (slotRepo.tryBook(slot) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "That pickup slot just filled up. Please choose another.");
        }
    }

    @Transactional
    public void release(LocalDateTime slot) {
        slotRepo.release(slot);
    }

    @Transactional(readOnly = true)
    public CanteenStatus status() {
        LocalDateTime now = LocalDateTime.now(clock);
        long inKitchen = orderRepo.countByStatusIn(OrderStatus.IN_KITCHEN);
        long ready = orderRepo.countByStatus(OrderStatus.READY);
        LocalDateTime next = upcoming().stream()
                .filter(s -> s.remaining() > 0)
                .map(SlotView::start)
                .findFirst().orElse(null);
        return new CanteenStatus(isOpenAt(now), props.getOpen().toString(), props.getClose().toString(),
                inKitchen, ready, (int) (inKitchen * props.getMinutesPerOrder()), next);
    }

    private boolean isOpenAt(LocalDateTime t) {
        return !t.toLocalTime().isBefore(props.getOpen()) && t.toLocalTime().isBefore(props.getClose());
    }

    private LocalDateTime roundUp(LocalDateTime t) {
        LocalDateTime minute = t.truncatedTo(ChronoUnit.MINUTES);
        if (minute.isBefore(t)) minute = minute.plusMinutes(1);
        int over = minute.getMinute() % props.getSlotMinutes();
        return over == 0 ? minute : minute.plusMinutes(props.getSlotMinutes() - over);
    }
}
