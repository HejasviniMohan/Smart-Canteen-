package com.canteen.controller;

import com.canteen.dto.AuthDtos;
import com.canteen.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @PostMapping("/register")
    @Operation(summary = "Register a new user (returns JWT)")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ResponseEntity.status(201).body(authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user (requires Bearer token)")
    public AuthDtos.UserSummary me(Authentication auth) {
        return authService.getProfile(auth.getName());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (stateless — client discards the token)")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}