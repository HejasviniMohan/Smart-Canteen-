package com.canteen.api;

import com.canteen.entity.FoodItem;
import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentMethod;
import com.canteen.service.OrderService;
import com.canteen.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderApiTest extends IntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory emf;

    @Test
    @DisplayName("The server charges the special price, and the menu reports that same price")
    void chargesEffectivePrice() throws Exception {
        FoodItem dosa = food("80.00", "65.00");
        send(get("/api/foods/" + dosa.getId()), null, null)
                .andExpect(jsonPath("$.effectivePrice").value(65.00));

        Actor student = student();
        send(post("/api/orders"), student, orderBody(dosa, 2, PaymentMethod.CASH_ON_PICKUP, SLOT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(130.00));
    }

    @Test
    @DisplayName("If the price changed since the student saw it, the order is refused")
    void refusesStalePrice() throws Exception {
        FoodItem food = food("100.00", null);
        Map<String, Object> body = new HashMap<>(orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, SLOT));
        body.put("expectedTotal", "90.00");
        send(post("/api/orders"), student(), body).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("An unpaid online order stays hidden from the kitchen until it is paid")
    void onlineOrderHiddenUntilPaid() throws Exception {
        Actor student = student();
        JsonNode order = placeOrder(student, food("50.00", null), PaymentMethod.ONLINE);
        long id = order.get("id").asLong();
        assertThat(order.get("status").asText()).isEqualTo("PENDING_PAYMENT");

        assertThat(activeOrderIds()).doesNotContain(id);
        send(put("/api/orders/" + id + "/status"), staff(), Map.of("status", "ACCEPTED"))
                .andExpect(status().isConflict());

        payOnline(student, id);

        assertThat(activeOrderIds()).contains(id);
        send(get("/api/orders/" + id), student, null)
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    @DisplayName("A cash order goes straight to the kitchen and records its payment method")
    void cashOrderGoesToKitchen() throws Exception {
        JsonNode order = placeOrder(student(), food("50.00", null), PaymentMethod.CASH_ON_PICKUP);
        assertThat(order.get("status").asText()).isEqualTo("PLACED");
        assertThat(order.get("paymentMethod").asText()).isEqualTo("CASH_ON_PICKUP");
        assertThat(activeOrderIds()).contains(order.get("id").asLong());
    }

    @Test
    @DisplayName("Status cannot skip steps or be set to PLACED by hand")
    void illegalTransitions() throws Exception {
        JsonNode order = placeOrder(student(), food("50.00", null), PaymentMethod.CASH_ON_PICKUP);
        String url = "/api/orders/" + order.get("id").asLong() + "/status";
        Actor staff = staff();

        send(put(url), staff, Map.of("status", "READY")).andExpect(status().isConflict());
        send(put(url), staff, Map.of("status", "PLACED")).andExpect(status().isConflict());
        send(put(url), staff, Map.of("status", "NONSENSE")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("An unpaid order cannot be completed, even through the status endpoint")
    void cannotCompleteUnpaid() throws Exception {
        JsonNode order = placeOrder(student(), food("50.00", null), PaymentMethod.CASH_ON_PICKUP);
        long id = order.get("id").asLong();
        Actor staff = staff();
        advanceToReady(id, staff);

        send(put("/api/orders/" + id + "/status"), staff, Map.of("status", "COMPLETED"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Payment is still pending for this order"));
    }

    @Test
    @DisplayName("Staff and admins can open any order; other students cannot")
    void orderVisibility() throws Exception {
        long id = placeOrder(student(), food("50.00", null), PaymentMethod.CASH_ON_PICKUP).get("id").asLong();
        send(get("/api/orders/" + id), staff(), null).andExpect(status().isOk());
        send(get("/api/orders/" + id), admin(), null).andExpect(status().isOk());
        send(get("/api/orders/" + id), student(), null).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Students can cancel only before cooking starts, and the slot place is freed")
    void cancelRules() throws Exception {
        Actor student = student();
        Actor staff = staff();
        FoodItem food = food("50.00", null);

        long early = placeOrder(student, food, PaymentMethod.CASH_ON_PICKUP).get("id").asLong();
        int bookedBefore = slots.findById(SLOT).orElseThrow().getBooked();
        send(delete("/api/orders/" + early), student, null).andExpect(status().isNoContent());
        assertThat(slots.findById(SLOT).orElseThrow().getBooked()).isLessThan(bookedBefore);
        send(delete("/api/orders/" + early), student, null).andExpect(status().isConflict());

        long late = placeOrder(student, food, PaymentMethod.CASH_ON_PICKUP).get("id").asLong();
        send(put("/api/orders/" + late + "/status"), staff, Map.of("status", "ACCEPTED"));
        send(put("/api/orders/" + late + "/status"), staff, Map.of("status", "PREPARING"));
        send(delete("/api/orders/" + late), student, null).andExpect(status().isConflict());

        advanceFromPreparing(late, staff);
        send(put("/api/orders/" + late + "/status"), staff, Map.of("status", "CANCELLED"))
                .andExpect(status().isConflict());
    }

    private void advanceFromPreparing(long id, Actor staff) throws Exception {
        send(put("/api/orders/" + id + "/status"), staff, Map.of("status", "READY")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Repeating a request with the same Idempotency-Key returns the original order")
    void idempotentRetry() throws Exception {
        Actor student = student();
        FoodItem food = food("50.00", null);
        Map<String, Object> body = orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, SLOT);

        long first = read(send(post("/api/orders").header("Idempotency-Key", "k1"), student, body)
                .andExpect(status().isCreated())).get("id").asLong();
        long second = read(send(post("/api/orders").header("Idempotency-Key", "k1"), student, body)
                .andExpect(status().isCreated())).get("id").asLong();

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("CONCURRENCY: 10 simultaneous identical requests create exactly one order and one booking")
    void idempotentUnderConcurrency() throws Exception {
        Actor student = student();
        FoodItem food = food("50.00", null);
        java.time.LocalDateTime slot = SLOT.plusHours(1);
        Map<String, Object> body = orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, slot);
        int bookedBefore = slots.findById(slot).map(s -> s.getBooked()).orElse(0);

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return read(send(post("/api/orders").header("Idempotency-Key", "double-click"), student, body)
                        .andExpect(status().isCreated())).get("id").asLong();
            }));
        }
        start.countDown();
        Set<Long> ids = new HashSet<>();
        for (Future<Long> f : results) ids.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(ids).hasSize(1);
        assertThat(slots.findById(slot).orElseThrow().getBooked()).isEqualTo(bookedBefore + 1);
    }

    @Test
    @DisplayName("Tokens count up from A001 within a business date")
    void tokensAreSequentialPerDay() throws Exception {
        Actor student = student();
        FoodItem food = food("50.00", null);
        java.time.LocalDateTime dayAfter = SLOT.plusDays(1).minusHours(1);

        String first = read(send(post("/api/orders"), student, orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, dayAfter))
                .andExpect(status().isCreated())).get("tokenNumber").asText();
        String second = read(send(post("/api/orders"), student, orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, dayAfter))
                .andExpect(status().isCreated())).get("tokenNumber").asText();

        assertThat(first).isEqualTo("A001");
        assertThat(second).isEqualTo("A002");
    }

    @Test
    @DisplayName("Abandoned online orders expire and give back their slot; paid ones are left alone")
    void unpaidOrdersExpire() throws Exception {
        Actor student = student();
        FoodItem food = food("50.00", null);
        long abandoned = placeOrder(student, food, PaymentMethod.ONLINE).get("id").asLong();
        long paid = placeOrder(student, food, PaymentMethod.ONLINE).get("id").asLong();
        payOnline(student, paid);
        int booked = slots.findById(SLOT).orElseThrow().getBooked();

        jdbc.update("UPDATE orders SET order_date = order_date - interval '1 hour' WHERE id IN (?, ?)", abandoned, paid);
        assertThat(orderService.expireUnpaidOrders()).isGreaterThanOrEqualTo(1);

        assertThat(orders.findById(abandoned).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orders.findById(paid).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(slots.findById(SLOT).orElseThrow().getBooked()).isLessThan(booked);
    }

    @Test
    @DisplayName("Deleted foods leave the menu but keep their order history; unavailable foods can't be ordered")
    void softDelete() throws Exception {
        Actor student = student();
        Actor admin = admin();
        FoodItem food = food("50.00", null);
        long orderId = placeOrder(student, food, PaymentMethod.CASH_ON_PICKUP).get("id").asLong();

        send(delete("/api/foods/" + food.getId()), admin, null).andExpect(status().isNoContent());

        send(get("/api/foods/" + food.getId()), null, null).andExpect(status().isNotFound());
        send(get("/api/orders/" + orderId), student, null)
                .andExpect(jsonPath("$.items[0].foodItemName").value(food.getName()));
        send(post("/api/orders"), student, orderBody(food, 1, PaymentMethod.CASH_ON_PICKUP, SLOT))
                .andExpect(status().isNotFound());

        FoodItem off = food("50.00", null);
        send(put("/api/foods/" + off.getId() + "/availability"), staff(), null).andExpect(status().isOk());
        send(post("/api/orders"), student, orderBody(off, 1, PaymentMethod.CASH_ON_PICKUP, SLOT))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Special price must be below the regular price and can be removed")
    void specialPriceRules() throws Exception {
        Actor admin = admin();
        FoodItem food = food("50.00", "40.00");
        send(put("/api/foods/" + food.getId()), admin, Map.of("specialPrice", "60.00"))
                .andExpect(status().isBadRequest());
        send(put("/api/foods/" + food.getId()), admin, Map.of("clearSpecialPrice", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectivePrice").value(50.00));
    }

    @Test
    @DisplayName("PERFORMANCE: the kitchen list's query count doesn't grow with the number of orders (no N+1)")
    void activeListHasNoNPlusOne() throws Exception {
        Actor student = student();
        FoodItem food = food("50.00", null);
        for (int i = 0; i < 15; i++) placeOrder(student, food, PaymentMethod.CASH_ON_PICKUP);
        Actor staff = staff();

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        send(get("/api/orders/active"), staff, null).andExpect(status().isOk());

        // One for auth, then a handful of batched loads, however many orders exist.
        assertThat(stats.getPrepareStatementCount()).isLessThan(10);
    }

    @Test
    @DisplayName("The admin order list is paginated")
    void adminListPaginated() throws Exception {
        placeOrder(student(), food("50.00", null), PaymentMethod.CASH_ON_PICKUP);
        send(get("/api/orders?page=0&size=1"), admin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalItems").isNumber());
    }

    private List<Long> activeOrderIds() throws Exception {
        JsonNode list = read(send(get("/api/orders/active"), staff(), null).andExpect(status().isOk()));
        List<Long> ids = new ArrayList<>();
        list.forEach(o -> ids.add(o.get("id").asLong()));
        return ids;
    }
}
