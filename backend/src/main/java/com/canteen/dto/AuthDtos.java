package com.canteen.dto;

import com.canteen.entity.Role;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class AuthDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RegisterRequest {
        @NotBlank @Size(max = 100)
        private String name;
        @NotBlank @Email @Size(max = 150)
        private String email;
        @NotBlank @Size(min = 6, max = 100)
        private String password;
        @Pattern(regexp = "^[0-9+\\- ]{6,20}$", message = "Invalid phone format")
        private String phone;
        @NotNull
        private Role role;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String token;
        private String tokenType;
        private long expiresInMs;
        private Long userId;
        private String name;
        private String email;
        private Role role;
        private LocalDateTime issuedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private Role role;
    }
}