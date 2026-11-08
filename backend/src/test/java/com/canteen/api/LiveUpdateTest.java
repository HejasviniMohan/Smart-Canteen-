package com.canteen.api;

import com.canteen.entity.*;
import com.canteen.realtime.OrderChangedEvent;
import com.canteen.repository.*;
import com.canteen.security.JwtService;
import com.canteen.support.PostgresContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Uses a real HTTP server and client: streaming can't be proven through MockMvc. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LiveUpdateTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresContainer.register(registry);
    }

    @LocalServerPort private int port;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private FoodItemRepository foods;
    @Autowired private JwtService jwt;
    @Autowired private ApplicationEventPublisher events;
    @Autowired private TransactionTemplate tx;

    private final HttpClient http = HttpClient.newHttpClient();

    private User user(Role role) {
        return users.save(User.builder().name(role.name())
                .email(UUID.randomUUID() + "@test.com").password("x").role(role).enabled(true).build());
    }

    /** Opens the stream and queues each "data:" line for the test to inspect. */
    private BlockingQueue<String> openStream(User user, ExecutorService pool) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/events/stream"))
                .header("Authorization", "Bearer " + jwt.generateToken(user)).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);

        BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        pool.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) lines.add(line);
                }
            }
            return null;
        });
        assertThat(lines.poll(5, TimeUnit.SECONDS)).as("connected event").contains("ok");
        return lines;
    }

    @Test
    @DisplayName("The stream requires authentication")
    void requiresAuth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/events/stream"))
                .GET().build();
        assertThat(http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Placing an order reaches staff and its owner, but not other students")
    void orderReachesTheRightPeople() throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        User owner = user(Role.STUDENT);
        BlockingQueue<String> staffStream = openStream(user(Role.STAFF), pool);
        BlockingQueue<String> ownerStream = openStream(owner, pool);
        BlockingQueue<String> otherStream = openStream(user(Role.STUDENT), pool);

        Category category = categories.save(Category.builder().name(UUID.randomUUID().toString()).build());
        FoodItem food = foods.save(FoodItem.builder().category(category).name("Dosa").price(new BigDecimal("50")).build());
        // A slot no other test touches: SlotApiTest shrinks some slots to capacity 1.
        String slot = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).atTime(16, 30).toString();
        String body = """
                {"items":[{"foodItemId":%d,"quantity":1}],"paymentMethod":"CASH_ON_PICKUP","pickupSlot":"%s"}
                """.formatted(food.getId(), slot);
        HttpResponse<String> placed = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/orders"))
                .header("Authorization", "Bearer " + jwt.generateToken(owner))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(placed.statusCode()).isEqualTo(201);

        assertThat(staffStream.poll(5, TimeUnit.SECONDS)).contains("PLACED");
        assertThat(ownerStream.poll(5, TimeUnit.SECONDS)).contains("PLACED");
        assertThat(otherStream.poll(1, TimeUnit.SECONDS)).isNull();
        pool.shutdownNow();
    }

    @Test
    @DisplayName("A change that rolls back is never announced to anyone")
    void rollbackIsNotAnnounced() throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        BlockingQueue<String> staffStream = openStream(user(Role.STAFF), pool);

        tx.executeWithoutResult(status -> {
            events.publishEvent(new OrderChangedEvent(1L, "x@test.com", "A001", "PLACED", "PENDING"));
            status.setRollbackOnly();
        });
        assertThat(staffStream.poll(1, TimeUnit.SECONDS)).isNull();

        tx.executeWithoutResult(status ->
                events.publishEvent(new OrderChangedEvent(1L, "x@test.com", "A001", "READY", "PAID")));
        assertThat(staffStream.poll(5, TimeUnit.SECONDS)).contains("READY");
        pool.shutdownNow();
    }
}
