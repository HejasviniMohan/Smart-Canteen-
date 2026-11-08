package com.canteen.api;

import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentMethod;
import com.canteen.entity.PaymentStatus;
import com.canteen.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PickupApiTest extends IntegrationTest {

    private record ReadyOrder(Actor student, long id, String code) {}

    private ReadyOrder paidReadyOrder(Actor staff) throws Exception {
        Actor student = student();
        JsonNode order = placeOrder(student, food("50.00", null), PaymentMethod.ONLINE);
        long id = order.get("id").asLong();
        payOnline(student, id);
        advanceToReady(id, staff);
        String code = read(send(get("/api/orders/" + id), student, null)).get("pickupCode").asText();
        return new ReadyOrder(student, id, code);
    }

    private org.springframework.test.web.servlet.ResultActions lookup(Actor staff, String code) throws Exception {
        return send(post("/api/pickup/lookup"), staff, Map.of("code", code));
    }

    @Test
    @DisplayName("A genuine QR code finds the order, and the full handover succeeds")
    void scanAndCollect() throws Exception {
        Actor staff = staff();
        ReadyOrder o = paidReadyOrder(staff);

        lookup(staff, o.code()).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(o.id()));
        send(post("/api/pickup/" + o.id() + "/collect"), staff, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.collectedAt").exists());
        assertThat(orders.findById(o.id()).orElseThrow().getCollectedBy()).isEqualTo(staff.email());
    }

    @Test
    @DisplayName("SECURITY: forged and altered QR codes are rejected")
    void forgedCodes() throws Exception {
        Actor staff = staff();
        ReadyOrder o = paidReadyOrder(staff);
        ReadyOrder other = paidReadyOrder(staff);
        String[] parts = o.code().split("\\.");

        // Old guessable format, wrong signature, and a real signature on a different order id.
        lookup(staff, "SMARTCANTEEN|TOKEN:A001|ORDER:" + o.id()).andExpect(status().isBadRequest());
        lookup(staff, parts[0] + "." + parts[1] + "." + parts[2] + ".AAAA").andExpect(status().isBadRequest());
        lookup(staff, parts[0] + "." + other.id() + "." + parts[2] + "." + parts[3]).andExpect(status().isBadRequest());
        lookup(staff, "SC1.1.2").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Codes stay short, so the QR is less dense and scans reliably")
    void codeIsCompact() throws Exception {
        ReadyOrder o = paidReadyOrder(staff());
        org.assertj.core.api.Assertions.assertThat(o.code()).hasSizeLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("SECURITY: a used QR code cannot collect the order twice")
    void reusedCode() throws Exception {
        Actor staff = staff();
        ReadyOrder o = paidReadyOrder(staff);
        send(post("/api/pickup/" + o.id() + "/collect"), staff, null).andExpect(status().isOk());

        send(post("/api/pickup/" + o.id() + "/collect"), staff, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This order was already collected"));
    }

    @Test
    @DisplayName("CONCURRENCY: of 10 simultaneous scans, exactly one hands over the food")
    void concurrentScans() throws Exception {
        Actor staff = staff();
        ReadyOrder o = paidReadyOrder(staff);

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return send(post("/api/pickup/" + o.id() + "/collect"), staff, null)
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        int succeeded = 0, conflicts = 0;
        for (Future<Integer> f : results) {
            int status = f.get(30, TimeUnit.SECONDS);
            if (status == 200) succeeded++;
            if (status == 409) conflicts++;
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(1);
        assertThat(conflicts).isEqualTo(9);
    }

    @Test
    @DisplayName("The code goes only to the owner, not to staff lists or other users")
    void codeOnlyForOwner() throws Exception {
        Actor student = student();
        long id = placeOrder(student, food("50.00", null), PaymentMethod.CASH_ON_PICKUP).get("id").asLong();

        send(get("/api/orders/" + id), student, null).andExpect(jsonPath("$.pickupCode").exists());
        send(get("/api/orders/" + id), staff(), null).andExpect(jsonPath("$.pickupCode").doesNotExist());
    }

    @Test
    @DisplayName("Orders that aren't ready can't be collected, and say why")
    void notReady() throws Exception {
        long id = placeOrder(student(), food("50.00", null), PaymentMethod.CASH_ON_PICKUP).get("id").asLong();
        send(post("/api/pickup/" + id + "/collect"), staff(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This order is not ready for pickup. It is PLACED"));
    }

    @Test
    @DisplayName("Cash order: blocked until cash is recorded, then collectable, with a payment record")
    void cashFlow() throws Exception {
        Actor staff = staff();
        long id = placeOrder(student(), food("50.00", null), PaymentMethod.CASH_ON_PICKUP).get("id").asLong();

        send(post("/api/pickup/" + id + "/cash-received"), staff, null).andExpect(status().isConflict());

        advanceToReady(id, staff);
        send(post("/api/pickup/" + id + "/collect"), staff, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Payment is still pending for this order"));

        send(post("/api/pickup/" + id + "/cash-received"), staff, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        // Pressing the button twice must not create a second payment.
        send(post("/api/pickup/" + id + "/cash-received"), staff, null).andExpect(status().isOk());

        send(post("/api/pickup/" + id + "/collect"), staff, null).andExpect(status().isOk());

        assertThat(payments.findByOrderId(id).orElseThrow().getPaymentMethod()).isEqualTo("CASH");
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Online orders can't be marked as paid in cash")
    void onlineOrderNotCash() throws Exception {
        Actor staff = staff();
        Actor student = student();
        long id = placeOrder(student, food("50.00", null), PaymentMethod.ONLINE).get("id").asLong();
        send(post("/api/pickup/" + id + "/cash-received"), staff, null).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Manual fallback: today's token finds the order, and a blank lookup is refused")
    void tokenFallback() throws Exception {
        Actor staff = staff();
        send(post("/api/pickup/lookup"), staff, Map.of()).andExpect(status().isBadRequest());
        send(post("/api/pickup/lookup"), staff, Map.of("token", "Z999")).andExpect(status().isNotFound());

        // Needs a slot later today, which doesn't exist if tests run just before midnight.
        var today = slotService.upcoming().stream()
                .filter(s -> s.start().toLocalDate().equals(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))))
                .findFirst();
        org.junit.jupiter.api.Assumptions.assumeTrue(today.isPresent());

        Actor student = student();
        JsonNode order = read(send(post("/api/orders"), student,
                orderBody(food("50.00", null), 1, PaymentMethod.CASH_ON_PICKUP, today.get().start()))
                .andExpect(status().isCreated()));
        send(post("/api/pickup/lookup"), staff, Map.of("token", order.get("tokenNumber").asText().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.get("id").asLong()));
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.canteen.slots.SlotService slotService;
}
