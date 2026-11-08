package com.canteen.api;

import com.canteen.entity.FoodItem;
import com.canteen.entity.PaymentMethod;
import com.canteen.entity.PickupSlot;
import com.canteen.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SlotApiTest extends IntegrationTest {

    @Test
    @DisplayName("CONCURRENCY: 50 simultaneous checkouts for a 10-place slot produce exactly 10 orders")
    void noOverbooking() throws Exception {
        LocalDateTime slot = SLOT.minusHours(2);
        slots.save(PickupSlot.builder().slotStart(slot).capacity(10).booked(0).build());
        FoodItem food = food("50.00", null);
        long ordersBefore = orders.count();

        List<Actor> students = new ArrayList<>();
        for (int i = 0; i < 50; i++) students.add(student());

        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (Actor student : students) {
            results.add(pool.submit(() -> {
                start.await();
                return send(post("/api/orders"), student, orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, slot))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();

        int created = 0, full = 0;
        for (Future<Integer> f : results) {
            int status = f.get(60, TimeUnit.SECONDS);
            if (status == 201) created++;
            if (status == 409) full++;
        }
        pool.shutdown();

        assertThat(created).isEqualTo(10);
        assertThat(full).isEqualTo(40);
        assertThat(slots.findById(slot).orElseThrow().getBooked()).isEqualTo(10);
        assertThat(orders.count() - ordersBefore).isEqualTo(10);
    }

    @Test
    @DisplayName("A full slot says so; a cancellation frees the place for someone else")
    void fullThenFreed() throws Exception {
        LocalDateTime slot = SLOT.minusHours(3);
        slots.save(PickupSlot.builder().slotStart(slot).capacity(1).booked(0).build());
        FoodItem food = food("50.00", null);
        Actor first = student();

        long id = read(send(post("/api/orders"), first, orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, slot))
                .andExpect(status().isCreated())).get("id").asLong();
        send(post("/api/orders"), student(), orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, slot))
                .andExpect(status().isConflict());

        send(delete("/api/orders/" + id), first, null).andExpect(status().isNoContent());

        send(post("/api/orders"), student(), orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, slot))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A rejected order doesn't use up a slot place")
    void rollbackFreesPlace() throws Exception {
        LocalDateTime slot = SLOT.minusHours(4);
        slots.save(PickupSlot.builder().slotStart(slot).capacity(5).booked(0).build());
        FoodItem off = food("50.00", null);
        off.setAvailability(false);
        foods.save(off);

        send(post("/api/orders"), student(), orderBody(off, 1, PaymentMethod.CASH_ON_PICKUP, slot))
                .andExpect(status().isConflict());

        assertThat(slots.findById(slot).orElseThrow().getBooked()).isZero();
    }

    @Test
    @DisplayName("Past, misaligned and too-distant times are rejected")
    void invalidSlots() throws Exception {
        FoodItem food = food("50.00", null);
        Actor student = student();
        for (LocalDateTime bad : List.of(
                LocalDateTime.now().minusHours(3).withSecond(0).withNano(0).withMinute(0),
                SLOT.plusMinutes(7),
                SLOT.plusDays(30))) {
            send(post("/api/orders"), student, orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, bad))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("The public slot list shows aligned future slots with real remaining capacity")
    void slotList() throws Exception {
        JsonNode list = read(send(get("/api/canteen/slots"), null, null).andExpect(status().isOk()));
        assertThat(list.size()).isGreaterThan(0);
        for (JsonNode slot : list) {
            LocalDateTime start = LocalDateTime.parse(slot.get("start").asText());
            assertThat(start.getMinute() % 15).isZero();
            assertThat(slot.get("remaining").asInt()).isLessThanOrEqualTo(slot.get("capacity").asInt());
        }
    }

    @Test
    @DisplayName("Status reports real kitchen numbers, which change as orders arrive")
    void statusIsReal() throws Exception {
        long before = read(send(get("/api/canteen/status"), null, null)).get("ordersInKitchen").asLong();
        placeOrder(student(), food("50.00", null), PaymentMethod.CASH_ON_PICKUP);

        send(get("/api/canteen/status"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").isBoolean())
                .andExpect(jsonPath("$.ordersInKitchen").value((int) before + 1));
    }
}
