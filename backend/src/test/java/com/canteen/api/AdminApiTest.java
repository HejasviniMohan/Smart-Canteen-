package com.canteen.api;

import com.canteen.entity.PaymentMethod;
import com.canteen.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminApiTest extends IntegrationTest {

    @Test
    @DisplayName("REGRESSION: analytics queries run on Postgres (HOUR() used to fail here but pass on H2)")
    void analyticsOnPostgres() throws Exception {
        Actor student = student();
        Actor staff = staff();
        long id = placeOrder(student, food("50.00", null), PaymentMethod.CASH_ON_PICKUP).get("id").asLong();
        advanceToReady(id, staff);
        send(post("/api/pickup/" + id + "/cash-received"), staff, null).andExpect(status().isOk());

        Actor admin = admin();
        send(get("/api/admin/dashboard"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayOrders").isNumber());
        send(get("/api/admin/peak-hours?days=7"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hour").isNumber());
        send(get("/api/admin/top-foods?days=7"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalQuantity").isNumber());
        send(get("/api/admin/revenue-trend?days=7"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    @DisplayName("Admins create staff accounts, and those accounts can log in")
    void createStaff() throws Exception {
        String email = unique("newstaff") + "@test.com";
        send(post("/api/admin/users"), admin(), Map.of(
                "name", "New Staff", "email", email, "password", "staffpass1", "role", "STAFF"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"));

        send(post("/api/auth/login"), null, Map.of("email", email, "password", "staffpass1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    @DisplayName("Admins can disable others but not lock themselves out")
    void enableDisable() throws Exception {
        Actor admin = admin();
        Actor target = student();
        send(put("/api/admin/users/" + target.user().getId() + "/enabled"), admin, Map.of("enabled", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        send(get("/api/auth/me"), target, null).andExpect(status().isUnauthorized());

        send(put("/api/admin/users/" + admin.user().getId() + "/enabled"), admin, Map.of("enabled", false))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deleting a category that still has foods gives 409, not a raw database error")
    void categoryInUse() throws Exception {
        var food = food("10.00", null);
        send(delete("/api/categories/" + food.getCategory().getId()), admin(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This conflicts with existing data"));
    }
}
