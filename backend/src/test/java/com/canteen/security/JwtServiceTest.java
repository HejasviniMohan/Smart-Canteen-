package com.canteen.security;

import com.canteen.entity.Role;
import com.canteen.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-must-be-at-least-32-bytes-long-abc",
                60_000L);
        user = User.builder()
                .id(7L).email("alice@canteen.com").name("Alice")
                .role(Role.STUDENT).password("x").enabled(true).build();
    }

    @Test
    @DisplayName("generate + parse: round-trip returns email, uid, role")
    void roundTrip() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@canteen.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(7L);
        assertThat(jwtService.extractRole(token)).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("expired token is invalid")
    void expiredTokenIsInvalid() {
        JwtService shortJwt = new JwtService(
                "test-secret-key-must-be-at-least-32-bytes-long-abc",
                1L);
        String token = shortJwt.generateToken(user);

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(shortJwt.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("garbage token is invalid")
    void garbageTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not.a.real.jwt")).isFalse();
    }

    @Test
    @DisplayName("constructor rejects short secret")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}