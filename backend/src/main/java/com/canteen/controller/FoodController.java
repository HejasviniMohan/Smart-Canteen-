package com.canteen.controller;

import com.canteen.dto.FoodDtos;
import com.canteen.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
@Tag(name = "Foods", description = "Food item management")
public class FoodController {

    private final FoodService service;

    @GetMapping
    @Operation(summary = "List all food items (filterable by category, availability, today's special)")
    public List<FoodDtos.Response> findAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Boolean todaySpecial) {
        return service.findAll(categoryId, available, todaySpecial);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get food item by ID")
    public FoodDtos.Response findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create food item (ADMIN only)")
    public ResponseEntity<FoodDtos.Response> create(@Valid @RequestBody FoodDtos.CreateRequest req) {
        return ResponseEntity.status(201).body(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update food item (ADMIN only)")
    public FoodDtos.Response update(@PathVariable Long id, @Valid @RequestBody FoodDtos.UpdateRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Toggle food availability (ADMIN/STAFF)")
    public FoodDtos.Response toggleAvailability(@PathVariable Long id) {
        return service.toggleAvailability(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete food item (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}