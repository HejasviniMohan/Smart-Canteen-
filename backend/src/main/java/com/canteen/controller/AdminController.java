package com.canteen.controller;

import com.canteen.dto.AdminDtos;
import com.canteen.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard & analytics (ADMIN only)")
public class AdminController {

    private final AdminService service;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard summary stats")
    public AdminDtos.DashboardStats dashboard() { return service.dashboard(); }

    @GetMapping("/top-foods")
    @Operation(summary = "Top selling food items (last 30 days)")
    public List<AdminDtos.TopFood> topFoods() { return service.topFoods(); }

    @GetMapping("/peak-hours")
    @Operation(summary = "Hourly order counts (last 7 days)")
    public List<AdminDtos.PeakHour> peakHours() { return service.peakHours(); }
}