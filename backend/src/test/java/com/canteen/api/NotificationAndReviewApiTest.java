package com.canteen.api;

import com.canteen.entity.FoodItem;
import com.canteen.entity.PaymentMethod;
import com.canteen.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationAndReviewApiTest extends IntegrationTest {

    @Test
    @DisplayName("REGRESSION: listing notifications works with unread ones (this used to return 500)")
    void unreadNotificationsList() throws Exception {
        Actor student = student();
        placeOrder(student, food("50.00", null), PaymentMethod.CASH_ON_PICKUP);

        send(get("/api/notifications/me"), student, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andExpect(jsonPath("$[0].title").value("Order placed"));
    }

    @Test
    @DisplayName("SECURITY: nobody can mark another user's notification as read")
    void notificationOwnership() throws Exception {
        Actor owner = student();
        placeOrder(owner, food("50.00", null), PaymentMethod.CASH_ON_PICKUP);
        long id = read(send(get("/api/notifications/me"), owner, null)).get(0).get("id").asLong();

        send(put("/api/notifications/" + id + "/read"), student(), null).andExpect(status().isNotFound());
        send(put("/api/notifications/" + id + "/read"), owner, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("Mark all as read")
    void markAll() throws Exception {
        Actor owner = student();
        placeOrder(owner, food("50.00", null), PaymentMethod.CASH_ON_PICKUP);
        send(put("/api/notifications/me/read-all"), owner, null).andExpect(status().isOk());
        send(get("/api/notifications/me"), owner, null).andExpect(jsonPath("$[0].isRead").value(true));
    }

    private long completedOrder(Actor student, FoodItem food) throws Exception {
        Actor staff = staff();
        long id = placeOrder(student, food, PaymentMethod.CASH_ON_PICKUP).get("id").asLong();
        advanceToReady(id, staff);
        send(post("/api/pickup/" + id + "/cash-received"), staff, null).andExpect(status().isOk());
        send(post("/api/pickup/" + id + "/collect"), staff, null).andExpect(status().isOk());
        return id;
    }

    @Test
    @DisplayName("Reviews: allowed once after pickup, and feed the food's real rating")
    void reviewFlow() throws Exception {
        Actor student = student();
        FoodItem food = food("50.00", null);
        long id = completedOrder(student, food);
        Map<String, Object> review = Map.of("orderId", id, "foodItemId", food.getId(), "rating", 4, "comment", "Good");

        send(post("/api/reviews"), student, review).andExpect(status().isCreated());
        send(post("/api/reviews"), student, review).andExpect(status().isConflict());

        send(get("/api/foods/" + food.getId()), null, null)
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
        send(get("/api/reviews/food/" + food.getId()), null, null)
                .andExpect(jsonPath("$[0].comment").value("Good"));
    }

    @Test
    @DisplayName("Reviews are refused before pickup, on other people's orders, and for food not in the order")
    void reviewRules() throws Exception {
        Actor student = student();
        FoodItem food = food("50.00", null);

        long open = placeOrder(student, food, PaymentMethod.CASH_ON_PICKUP).get("id").asLong();
        send(post("/api/reviews"), student, Map.of("orderId", open, "foodItemId", food.getId(), "rating", 5))
                .andExpect(status().isConflict());

        long done = completedOrder(student, food);
        send(post("/api/reviews"), student(), Map.of("orderId", done, "foodItemId", food.getId(), "rating", 5))
                .andExpect(status().isForbidden());
        send(post("/api/reviews"), student, Map.of("orderId", done, "foodItemId", food("10.00", null).getId(), "rating", 5))
                .andExpect(status().isBadRequest());
        send(post("/api/reviews"), student, Map.of("orderId", done, "foodItemId", food.getId(), "rating", 9))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admins can moderate reviews")
    void moderation() throws Exception {
        Actor student = student();
        FoodItem food = food("50.00", null);
        long id = completedOrder(student, food);
        JsonNode created = read(send(post("/api/reviews"), student,
                Map.of("orderId", id, "foodItemId", food.getId(), "rating", 1, "comment", "Spam"))
                .andExpect(status().isCreated()));
        Actor admin = admin();

        send(get("/api/admin/reviews"), admin, null).andExpect(jsonPath("$.items[0].id").isNumber());
        send(delete("/api/admin/reviews/" + created.get("id").asLong()), admin, null)
                .andExpect(status().isNoContent());
        send(get("/api/foods/" + food.getId()), null, null).andExpect(jsonPath("$.reviewCount").value(0));
    }
}
