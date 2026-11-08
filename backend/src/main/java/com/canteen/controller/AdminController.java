package com.canteen.controller;

import com.canteen.dto.AdminDtos;
import com.canteen.dto.PageResponse;
import com.canteen.dto.PaymentDtos;
import com.canteen.dto.ReviewDtos;
import com.canteen.entity.PaymentStatus;
import com.canteen.service.AdminService;
import com.canteen.service.PaymentService;
import com.canteen.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard & analytics (ADMIN only)")
public class AdminController {

    private final AdminService service;
    private final PaymentService paymentService;
    private final ReviewService reviewService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard summary stats")
    public AdminDtos.DashboardStats dashboard() { return service.dashboard(); }

    @GetMapping("/top-foods")
    @Operation(summary = "Top selling food items in the last N days")
    public List<AdminDtos.TopFood> topFoods(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return service.topFoods(days);
    }

    @GetMapping("/peak-hours")
    @Operation(summary = "Hourly order counts in the last N days")
    public List<AdminDtos.PeakHour> peakHours(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return service.peakHours(days);
    }

    @GetMapping("/revenue-trend")
    @Operation(summary = "Daily revenue and orders in the last N days")
    public List<AdminDtos.RevenuePoint> revenueTrend(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return service.revenueTrend(days);
    }

    @GetMapping("/payments")
    @Operation(summary = "All payments, optionally filtered by status")
    public PageResponse<PaymentDtos.PaymentResponse> payments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return paymentService.list(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/reviews")
    @Operation(summary = "All reviews, newest first")
    public PageResponse<ReviewDtos.Response> reviews(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviewService.all(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Remove a review")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
