package com.canteen;

import com.canteen.entity.Role;
import com.canteen.entity.User;
import com.canteen.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Authentication flow integration")
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void wipeUsers() {
        userRepo.deleteAll();
        userRepo.save(User.builder()
                .name("Admin").email("admin@canteen.com")
                .password(encoder.encode("Admin@123"))
                .role(Role.ADMIN).enabled(true).build());
    }

    @Test
    @DisplayName("Register, login, then call /api/auth/me with the token")
    void registerLoginMeFlow() throws Exception {
        Map<String, Object> registerBody = Map.of(
                "name", "Alice",
                "email", "alice@x.com",
                "password", "secret123",
                "role", "STUDENT",
                "phone", "9999900002"
        );

        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("alice@x.com"))
                .andReturn();

        Map<?, ?> regBody = json.readValue(reg.getResponse().getContentAsString(), Map.class);
        String token = (String) regBody.get("token");
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@x.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("Login rejects bad password with 401")
    void loginBadPassword() throws Exception {
        Map<String, Object> body = Map.of(
                "email", "admin@canteen.com",
                "password", "WrongPass");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Login rejects unknown email with 401")
    void loginUnknownEmail() throws Exception {
        Map<String, Object> body = Map.of("email", "nobody@x.com", "password", "whatever");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Register rejects duplicate email with 409")
    void registerDuplicateEmail() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Admin",
                "email", "admin@canteen.com",
                "password", "secret123",
                "role", "ADMIN");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    @DisplayName("Register rejects bad payload (validation 400)")
    void registerValidation() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "",
                "email", "not-an-email",
                "password", "x",
                "role", "STUDENT");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/api/auth/me without token returns 403")
    void meWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }
}