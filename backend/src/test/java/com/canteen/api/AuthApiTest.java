package com.canteen.api;

import com.canteen.entity.Role;
import com.canteen.entity.User;
import com.canteen.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthApiTest extends IntegrationTest {

    private Map<String, Object> registration(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Alice");
        body.put("email", email);
        body.put("password", "secret123");
        return body;
    }

    @Test
    @DisplayName("Register, then call /me with the returned token")
    void registerThenMe() throws Exception {
        String email = unique("alice") + "@test.com";
        String token = read(send(post("/api/auth/register"), null, registration(email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))).get("token").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("SECURITY: a role sent to public sign-up is ignored, so nobody can self-register as admin")
    void cannotSelfRegisterAsAdmin() throws Exception {
        String email = unique("mallory") + "@test.com";
        Map<String, Object> body = registration(email);
        body.put("role", "ADMIN");

        send(post("/api/auth/register"), null, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        assertThat(users.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("Emails are case-insensitive, for both duplicates and login")
    void emailCaseInsensitive() throws Exception {
        String email = unique("casey") + "@test.com";
        send(post("/api/auth/register"), null, registration(email)).andExpect(status().isCreated());

        send(post("/api/auth/register"), null, registration(email.toUpperCase()))
                .andExpect(status().isConflict());
        send(post("/api/auth/login"), null, Map.of("email", email.toUpperCase(), "password", "secret123"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Weak passwords and bad payloads are rejected with 400")
    void validation() throws Exception {
        Map<String, Object> weak = registration(unique("weak") + "@test.com");
        weak.put("password", "abcdefgh");
        send(post("/api/auth/register"), null, weak).andExpect(status().isBadRequest());

        send(post("/api/auth/register"), null, Map.of("name", "", "email", "nope", "password", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Wrong password, unknown email and disabled account all give the same 401")
    void loginFailuresLookTheSame() throws Exception {
        String password = "secret123";
        User disabled = users.save(User.builder().name("D").email(unique("disabled") + "@test.com")
                .password(encoder.encode(password)).role(Role.STUDENT).enabled(false).build());
        User active = users.save(User.builder().name("A").email(unique("active") + "@test.com")
                .password(encoder.encode(password)).role(Role.STUDENT).enabled(true).build());

        for (Map<String, Object> attempt : java.util.List.<Map<String, Object>>of(
                Map.of("email", active.getEmail(), "password", "wrong-pass1"),
                Map.of("email", "nobody@test.com", "password", password),
                Map.of("email", disabled.getEmail(), "password", password))) {
            send(post("/api/auth/login"), null, attempt)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }

    @Test
    @DisplayName("Missing or invalid tokens get 401, so the frontend knows to log the user out")
    void unauthenticatedIs401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SECURITY: disabling a user invalidates their existing token immediately")
    void disabledUsersTokenStopsWorking() throws Exception {
        Actor student = student();
        send(get("/api/auth/me"), student, null).andExpect(status().isOk());

        User user = student.user();
        user.setEnabled(false);
        users.save(user);

        send(get("/api/auth/me"), student, null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Profile update and password change")
    void profileAndPassword() throws Exception {
        String email = unique("pat") + "@test.com";
        String token = read(send(post("/api/auth/register"), null, registration(email))).get("token").asText();
        Actor pat = new Actor(users.findByEmail(email).orElseThrow(), token);

        send(put("/api/auth/me"), pat, Map.of("name", "Pat", "phone", "9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pat"));

        send(put("/api/auth/me/password"), pat, Map.of("currentPassword", "wrong", "newPassword", "newsecret1"))
                .andExpect(status().isBadRequest());
        send(put("/api/auth/me/password"), pat, Map.of("currentPassword", "secret123", "newPassword", "newsecret1"))
                .andExpect(status().isNoContent());

        send(post("/api/auth/login"), null, Map.of("email", email, "password", "newsecret1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Internal error details never reach the client")
    void errorsDoNotLeakInternals() throws Exception {
        send(get("/api/orders/not-a-number"), student(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request"));
        send(get("/api/nothing-here"), student(), null).andExpect(status().isNotFound());
    }
}
