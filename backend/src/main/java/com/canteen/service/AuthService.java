package com.canteen.service;

import com.canteen.dto.AuthDtos;
import com.canteen.entity.Role;
import com.canteen.entity.User;
import com.canteen.exception.ApiException;
import com.canteen.repository.UserRepository;
import com.canteen.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BAD_LOGIN = "Invalid email or password";

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (userRepo.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .name(req.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(blankToNull(req.getPhone()))
                .role(Role.STUDENT)
                .enabled(true)
                .build();
        userRepo.save(user);
        return buildAuthResponse(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String email = normalizeEmail(req.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, req.getPassword()));
        } catch (AuthenticationException ex) {
            // Wrong password, unknown email and disabled account all look the same from outside.
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_LOGIN);
        }
        User user = userRepo.findByEmail(email)
                .filter(u -> !Boolean.FALSE.equals(u.getEnabled()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, BAD_LOGIN));
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserSummary getProfile(String email) {
        return toSummary(getUser(email));
    }

    @Transactional
    public AuthDtos.UserSummary updateProfile(String email, AuthDtos.UpdateProfileRequest req) {
        User u = getUser(email);
        u.setName(req.getName().trim());
        u.setPhone(blankToNull(req.getPhone()));
        return toSummary(u);
    }

    @Transactional
    public void changePassword(String email, AuthDtos.ChangePasswordRequest req) {
        User u = getUser(email);
        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
    }

    public static AuthDtos.UserSummary toSummary(User u) {
        return AuthDtos.UserSummary.builder()
                .id(u.getId()).name(u.getName()).email(u.getEmail())
                .phone(u.getPhone()).role(u.getRole()).enabled(u.getEnabled()).build();
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthDtos.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .issuedAt(LocalDateTime.now())
                .build();
    }
}
