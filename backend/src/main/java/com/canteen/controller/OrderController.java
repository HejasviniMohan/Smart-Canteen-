package com.canteen.controller;

import com.canteen.dto.OrderDtos;
import com.canteen.dto.PageResponse;
import com.canteen.entity.OrderStatus;
import com.canteen.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Orders", description = "Order placement, tracking, status updates")
public class OrderController {

    private final OrderService service;

    public static boolean isStaff(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Place a new order. Repeating a request with the same Idempotency-Key returns the original")
    public ResponseEntity<OrderDtos.OrderResponse> place(
            Authentication auth,
            @RequestHeader(value = "Idempotency-Key", required = false) @Size(max = 80) String idempotencyKey,
            @Valid @RequestBody OrderDtos.PlaceOrderRequest req) {
        try {
            return ResponseEntity.status(201).body(service.placeOrder(auth.getName(), req, idempotencyKey));
        } catch (DataIntegrityViolationException e) {
            // An identical request won the race. Its transaction has committed, so return its order.
            if (idempotencyKey != null) {
                return service.findByIdempotencyKey(auth.getName(), idempotencyKey)
                        .map(o -> ResponseEntity.status(201).body(o))
                        .orElseThrow(() -> e);
            }
            throw e;
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "My latest orders (STUDENT)")
    public List<OrderDtos.OrderResponse> mine(Authentication auth) {
        return service.myOrders(auth.getName());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Paid or cash orders the kitchen should work on, by pickup time (STAFF/ADMIN)")
    public List<OrderDtos.OrderResponse> active() {
        return service.activeOrders();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by id (owner or STAFF/ADMIN)")
    public OrderDtos.OrderResponse getById(Authentication auth, @PathVariable Long id) {
        return service.getById(auth.getName(), isStaff(auth), id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "All orders, newest first, with optional status filter (STAFF/ADMIN)")
    public PageResponse<OrderDtos.OrderResponse> all(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.allOrders(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate")));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Update order status (STAFF/ADMIN)")
    public OrderDtos.OrderResponse updateStatus(Authentication auth, @PathVariable Long id,
                                                @Valid @RequestBody OrderDtos.StatusUpdateRequest req) {
        return service.updateStatus(id, req.getStatus(), auth.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Cancel my order, with a refund if it was paid (STUDENT)")
    public ResponseEntity<Void> cancel(Authentication auth, @PathVariable Long id) {
        service.cancelOrder(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
