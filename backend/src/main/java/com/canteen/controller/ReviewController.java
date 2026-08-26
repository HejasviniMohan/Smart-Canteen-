package com.canteen.controller;

import com.canteen.dto.ReviewDtos;
import com.canteen.service.ReviewService;
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
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Food reviews")
public class ReviewController {

    private final ReviewService service;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create a review (STUDENT, only after order COMPLETED)")
    public ResponseEntity<ReviewDtos.Response> create(Authentication auth,
                                                      @Valid @RequestBody ReviewDtos.CreateRequest req) {
        return ResponseEntity.status(201).body(service.create(auth.getName(), req));
    }

    @GetMapping("/food/{foodId}")
    @Operation(summary = "Reviews for a food item (public)")
    public List<ReviewDtos.Response> forFood(@PathVariable Long foodId) {
        return service.forFood(foodId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "My reviews (STUDENT)")
    public List<ReviewDtos.Response> mine(Authentication auth) {
        return service.myReviews(auth.getName());
    }
}