package com.canteen.api;

import com.canteen.entity.Role;
import com.canteen.support.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Role-by-endpoint table. "open" means the role passes the security check; the handler may
 * then answer 200, 400 or 404, but never 401 or 403. Each role is checked separately.
 */
class AuthorizationMatrixTest extends IntegrationTest {

    @ParameterizedTest(name = "{0} {1}: anon={2} student={3} staff={4} admin={5}")
    @CsvSource({
            // method, path,                 anonymous, student, staff, admin
            "GET,    /api/foods,                 open, open, open, open",
            "GET,    /api/canteen/status,        open, open, open, open",
            "GET,    /api/canteen/slots,         open, open, open, open",
            "POST,   /api/foods,                 401,  403,  403,  open",
            "DELETE, /api/foods/999999,          401,  403,  403,  open",
            "PUT,    /api/foods/999999/availability, 401, 403, open, open",
            "POST,   /api/categories,            401,  403,  403,  open",
            "POST,   /api/orders,                401,  open, 403,  403",
            "GET,    /api/orders/me,             401,  open, 403,  403",
            "GET,    /api/orders,                401,  403,  open, open",
            "GET,    /api/orders/active,         401,  403,  open, open",
            "PUT,    /api/orders/999999/status,  401,  403,  open, open",
            "DELETE, /api/orders/999999,         401,  open, 403,  403",
            "POST,   /api/payments/create-order, 401,  open, 403,  403",
            "POST,   /api/payments/verify,       401,  open, 403,  403",
            "POST,   /api/pickup/lookup,         401,  403,  open, open",
            "POST,   /api/pickup/999999/collect, 401,  403,  open, open",
            "POST,   /api/pickup/999999/cash-received, 401, 403, open, open",
            "GET,    /api/admin/users,           401,  403,  403,  open",
            "POST,   /api/admin/users,           401,  403,  403,  open",
            "PUT,    /api/admin/users/999999/enabled, 401, 403, 403, open",
            "GET,    /api/admin/dashboard,       401,  403,  403,  open",
            "GET,    /api/admin/payments,        401,  403,  403,  open",
            "GET,    /api/admin/reviews,         401,  403,  403,  open",
            "DELETE, /api/admin/reviews/999999,  401,  403,  403,  open",
            "GET,    /api/notifications/me,      401,  open, open, open",
            "POST,   /api/reviews,               401,  open, 403,  403",
    })
    void accessRules(String method, String path, String anonymous, String student, String staff, String admin)
            throws Exception {
        check(method, path, null, anonymous);
        check(method, path, Role.STUDENT, student);
        check(method, path, Role.STAFF, staff);
        check(method, path, Role.ADMIN, admin);
    }

    /** Bodies must be valid, because Spring validates them before checking @PreAuthorize. */
    private String validBody(String path) {
        if (path.endsWith("/status")) return "{\"status\":\"ACCEPTED\"}";
        if (path.endsWith("/enabled")) return "{\"enabled\":true}";
        return switch (path) {
            case "/api/foods" -> "{\"categoryId\":999999,\"name\":\"x\",\"price\":10}";
            case "/api/categories" -> "{\"name\":\"" + unique("matrix") + "\"}";
            case "/api/orders" -> """
                    {"items":[{"foodItemId":999999,"quantity":1}],"paymentMethod":"ONLINE","pickupSlot":"%s"}
                    """.formatted(SLOT);
            case "/api/payments/create-order" -> "{\"orderId\":999999}";
            case "/api/payments/verify" -> """
                    {"paymentId":999999,"razorpayOrderId":"a","razorpayPaymentId":"b","razorpaySignature":"c"}
                    """;
            case "/api/admin/users" -> """
                    {"name":"x","email":"%s@test.com","password":"password1","role":"STAFF"}
                    """.formatted(unique("matrix"));
            case "/api/reviews" -> "{\"orderId\":999999,\"foodItemId\":999999,\"rating\":5}";
            default -> "{}";
        };
    }

    private void check(String method, String path, Role role, String expected) throws Exception {
        var request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path);
        if (role != null) request.header("Authorization", "Bearer " + actor(role).token());
        if (!method.equals("GET") && !method.equals("DELETE")) {
            request.contentType("application/json").content(validBody(path));
        }
        int status = mvc.perform(request).andReturn().getResponse().getStatus();
        String who = (role == null ? "anonymous" : role.name()) + " " + method + " " + path;
        if (expected.equals("open")) {
            assertThat(status).as(who).isNotIn(401, 403);
        } else {
            assertThat(status).as(who).isEqualTo(Integer.parseInt(expected));
        }
    }
}
