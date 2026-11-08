package com.canteen.dto;

import com.canteen.entity.Role;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class AuthDtos {

    private static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$";
    private static final String PASSWORD_MESSAGE = "Password must be at least 8 characters with a letter and a number";

    /** Has no role field on purpose: public sign-up can only ever create a STUDENT. */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RegisterRequest {
        @NotBlank @Size(max = 100)
        private String name;
        @NotBlank @Email @Size(max = 150)
        private String email;
        @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE)
        private String password;
        @Pattern(regexp = "^$|^[0-9+\\- ]{6,20}$", message = "Invalid phone format")
        private String phone;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateProfileRequest {
        @NotBlank @Size(max = 100)
        private String name;
        @Pattern(regexp = "^$|^[0-9+\\- ]{6,20}$", message = "Invalid phone format")
        private String phone;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE)
        private String newPassword;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateUserRequest {
        @NotBlank @Size(max = 100)
        private String name;
        @NotBlank @Email @Size(max = 150)
        private String email;
        @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE)
        private String password;
        @Pattern(regexp = "^$|^[0-9+\\- ]{6,20}$", message = "Invalid phone format")
        private String phone;
        @NotNull
        private Role role;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SetEnabledRequest {
        @NotNull
        private Boolean enabled;
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
        private Boolean enabled;
    }
}
