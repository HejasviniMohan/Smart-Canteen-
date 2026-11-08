package com.canteen.service;

import com.canteen.dto.AuthDtos;
import com.canteen.entity.Role;
import com.canteen.entity.User;
import com.canteen.exception.ApiException;
import com.canteen.repository.UserRepository;
import com.canteen.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtService jwt;
    @Mock private AuthenticationManager authManager;

    @InjectMocks private AuthService service;

    private User storedStudent;

    @BeforeEach
    void setUp() {
        storedStudent = User.builder()
                .id(1L).name("Alice").email("alice@x.com")
                .password("hashed").role(Role.STUDENT).enabled(true).build();
    }

    @Test
    @DisplayName("register: persists user with encoded password and returns token")
    void register_persists() {
        AuthDtos.RegisterRequest req = AuthDtos.RegisterRequest.builder()
                .name("Bob").email("bob@x.com").password("secret1")
                .phone("9999900000").build();

        when(userRepo.existsByEmail("bob@x.com")).thenReturn(false);
        when(encoder.encode("secret1")).thenReturn("hashed");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(jwt.generateToken(any(User.class))).thenReturn("jwt.token.here");
        when(jwt.getExpirationMs()).thenReturn(86_400_000L);

        AuthDtos.AuthResponse resp = service.register(req);

        assertThat(resp.getToken()).isEqualTo("jwt.token.here");
        assertThat(resp.getUserId()).isEqualTo(99L);
        assertThat(resp.getEmail()).isEqualTo("bob@x.com");
        assertThat(resp.getRole()).isEqualTo(Role.STUDENT);
        verify(userRepo).save(any(User.class));
    }

    @Test
    @DisplayName("register: rejects duplicate email with 409")
    void register_rejectsDuplicateEmail() {
        AuthDtos.RegisterRequest req = AuthDtos.RegisterRequest.builder()
                .name("Bob").email("bob@x.com").password("secret1").build();

        when(userRepo.existsByEmail("bob@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("login: returns token on valid credentials")
    void login_returnsToken() {
        AuthDtos.LoginRequest req = AuthDtos.LoginRequest.builder()
                .email("alice@x.com").password("raw").build();

        when(userRepo.findByEmail("alice@x.com")).thenReturn(Optional.of(storedStudent));
        when(jwt.generateToken(storedStudent)).thenReturn("jwt.ok");
        when(jwt.getExpirationMs()).thenReturn(86_400_000L);

        AuthDtos.AuthResponse resp = service.login(req);

        assertThat(resp.getToken()).isEqualTo("jwt.ok");
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: bad credentials -> 401")
    void login_badCreds() {
        AuthDtos.LoginRequest req = AuthDtos.LoginRequest.builder()
                .email("alice@x.com").password("wrong").build();

        doThrow(new BadCredentialsException("nope"))
                .when(authManager).authenticate(any());

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("login: disabled account -> same 401 as any other failure")
    void login_disabledAccount() {
        storedStudent.setEnabled(false);
        AuthDtos.LoginRequest req = AuthDtos.LoginRequest.builder()
                .email("alice@x.com").password("raw").build();

        when(userRepo.findByEmail("alice@x.com")).thenReturn(Optional.of(storedStudent));

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}