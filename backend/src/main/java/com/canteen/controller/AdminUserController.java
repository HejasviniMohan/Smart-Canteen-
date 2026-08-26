package com.canteen.controller;

import com.canteen.dto.AuthDtos;
import com.canteen.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin user listing")
public class AdminUserController {

    private final UserRepository userRepo;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List all users (ADMIN)")
    public List<AuthDtos.UserSummary> all() {
        return userRepo.findAll().stream().map(u ->
                AuthDtos.UserSummary.builder()
                        .id(u.getId()).name(u.getName()).email(u.getEmail())
                        .phone(u.getPhone()).role(u.getRole()).build()).toList();
    }
}