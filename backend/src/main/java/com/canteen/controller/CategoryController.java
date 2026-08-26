package com.canteen.controller;

import com.canteen.dto.CategoryDtos;
import com.canteen.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Food category management")
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    @Operation(summary = "List all categories (public)")
    public List<CategoryDtos.Response> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public CategoryDtos.Response findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category (ADMIN only)")
    public ResponseEntity<CategoryDtos.Response> create(@Valid @RequestBody CategoryDtos.CreateRequest req) {
        return ResponseEntity.status(201).body(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category (ADMIN only)")
    public CategoryDtos.Response update(@PathVariable Long id, @Valid @RequestBody CategoryDtos.UpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}