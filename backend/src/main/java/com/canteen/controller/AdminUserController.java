package com.canteen.controller;

import com.canteen.dto.AuthDtos;
import com.canteen.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "User management (ADMIN only)")
public class AdminUserController {

    private final AdminService service;

    @GetMapping
    @Operation(summary = "List all users")
    public List<AuthDtos.UserSummary> all() {
        return service.users();
    }

    @PostMapping
    @Operation(summary = "Create a staff or admin account")
    public ResponseEntity<AuthDtos.UserSummary> create(@Valid @RequestBody AuthDtos.CreateUserRequest req) {
        return ResponseEntity.status(201).body(service.createUser(req));
    }

    @PutMapping("/{id}/enabled")
    @Operation(summary = "Enable or disable a user")
    public AuthDtos.UserSummary setEnabled(Authentication auth, @PathVariable Long id,
                                           @Valid @RequestBody AuthDtos.SetEnabledRequest req) {
        return service.setEnabled(id, req.getEnabled(), auth.getName());
    }
}
