package com.canteen.controller;

import com.canteen.dto.AuthDtos;
import com.canteen.security.LoginRateLimiter;
import com.canteen.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, profile")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    @PostMapping("/register")
    @Operation(summary = "Register a new student account (returns JWT)")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req,
                                                          HttpServletRequest http) {
        rateLimiter.check(req.getEmail(), http.getRemoteAddr());
        return ResponseEntity.status(201).body(authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest req, HttpServletRequest http) {
        rateLimiter.check(req.getEmail(), http.getRemoteAddr());
        return authService.login(req);
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user")
    public AuthDtos.UserSummary me(Authentication auth) {
        return authService.getProfile(auth.getName());
    }

    @PutMapping("/me")
    @Operation(summary = "Update my name and phone")
    public AuthDtos.UserSummary updateMe(Authentication auth, @Valid @RequestBody AuthDtos.UpdateProfileRequest req) {
        return authService.updateProfile(auth.getName(), req);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change my password")
    public ResponseEntity<Void> changePassword(Authentication auth, @Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        authService.changePassword(auth.getName(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (stateless: the client discards the token)")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
