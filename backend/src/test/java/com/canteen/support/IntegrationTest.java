package com.canteen.support;

import com.canteen.entity.*;
import com.canteen.repository.*;
import com.canteen.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against real Postgres in a container shared by all test classes, the same database
 * as production, so migrations and SQL are exercised for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresContainer.register(registry);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected CategoryRepository categories;
    @Autowired protected FoodItemRepository foods;
    @Autowired protected OrderRepository orders;
    @Autowired protected PaymentRepository payments;
    @Autowired protected PickupSlotRepository slots;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected JwtService jwt;

    /** Slots tomorrow are always in the future and open, whatever time the tests run. */
    protected static final LocalDateTime SLOT = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))
            .plusDays(1).atTime(12, 0);

    public record Actor(User user, String token) {
        public String email() { return user.getEmail(); }
    }

    protected static String unique(String prefix) {
        return prefix + "-" + SEQUENCE.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected Actor actor(Role role) {
        User user = users.save(User.builder()
                .name(role + " user")
                .email(unique(role.name().toLowerCase()) + "@test.com")
                .password("not-used")
                .role(role)
                .enabled(true)
                .build());
        return new Actor(user, jwt.generateToken(user));
    }

    protected Actor student() { return actor(Role.STUDENT); }
    protected Actor staff() { return actor(Role.STAFF); }
    protected Actor admin() { return actor(Role.ADMIN); }

    protected FoodItem food(String price, String specialPrice) {
        Category category = categories.save(Category.builder().name(unique("cat")).build());
        return foods.save(FoodItem.builder()
                .category(category)
                .name(unique("food"))
                .price(new BigDecimal(price))
                .specialPrice(specialPrice == null ? null : new BigDecimal(specialPrice))
                .build());
    }

    protected ResultActions send(MockHttpServletRequestBuilder request, Actor as, Object body) throws Exception {
        if (as != null) request.header("Authorization", "Bearer " + as.token());
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(request);
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected Map<String, Object> orderBody(FoodItem food, int quantity, PaymentMethod method, LocalDateTime slot) {
        return Map.of(
                "items", List.of(Map.of("foodItemId", food.getId(), "quantity", quantity)),
                "paymentMethod", method.name(),
                "pickupSlot", slot.toString());
    }

    protected JsonNode placeOrder(Actor student, FoodItem food, PaymentMethod method) throws Exception {
        return read(send(post("/api/orders"), student, orderBody(food, 1, method, SLOT))
                .andExpect(status().isCreated()));
    }

    /** Walks a PLACED order to READY as the kitchen would. */
    protected void advanceToReady(long orderId, Actor staff) throws Exception {
        for (String next : List.of("ACCEPTED", "PREPARING", "READY")) {
            send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/api/orders/" + orderId + "/status"), staff, Map.of("status", next))
                    .andExpect(status().isOk());
        }
    }

    /** Pays through the same endpoints as the real checkout, using the sandbox provider. */
    protected JsonNode payOnline(Actor student, long orderId) throws Exception {
        JsonNode checkout = read(send(post("/api/payments/create-order"), student, Map.of("orderId", orderId))
                .andExpect(status().isCreated()));
        long paymentId = checkout.get("paymentId").asLong();
        JsonNode signed = read(send(post("/api/payments/sandbox/pay"), student,
                Map.of("paymentId", paymentId, "success", true)).andExpect(status().isOk()));
        return read(send(post("/api/payments/verify"), student, Map.of(
                "paymentId", paymentId,
                "razorpayOrderId", signed.get("razorpayOrderId").asText(),
                "razorpayPaymentId", signed.get("razorpayPaymentId").asText(),
                "razorpaySignature", signed.get("razorpaySignature").asText()))
                .andExpect(status().isOk()));
    }
}
