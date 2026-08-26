package com.canteen.controller;

import com.canteen.dto.OrderDtos;
import com.canteen.entity.OrderStatus;
import com.canteen.service.OrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, tracking, status updates")
public class OrderController {

    private final OrderService service;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Place a new order (STUDENT)")
    public ResponseEntity<OrderDtos.OrderResponse> place(Authentication auth,
                                                          @Valid @RequestBody OrderDtos.PlaceOrderRequest req) {
        return ResponseEntity.status(201).body(service.placeOrder(auth.getName(), req));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "My orders (STUDENT)")
    public List<OrderDtos.OrderResponse> mine(Authentication auth) {
        return service.myOrders(auth.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by id (owner or STAFF/ADMIN)")
    public OrderDtos.OrderResponse getById(Authentication auth, @PathVariable Long id) {
        return service.getById(auth.getName(), id);
    }

    @GetMapping("/token/{token}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Find order by token number (STAFF/ADMIN - for QR verification)")
    public OrderDtos.OrderResponse getByToken(@PathVariable String token) {
        return service.getByToken(token);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "All orders, optional status filter (STAFF/ADMIN)")
    public List<OrderDtos.OrderResponse> all(@RequestParam(required = false) OrderStatus status) {
        return service.allOrders(status);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Update order status (STAFF/ADMIN)")
    public OrderDtos.OrderResponse updateStatus(@PathVariable Long id,
                                                @Valid @RequestBody OrderDtos.StatusUpdateRequest req) {
        return service.updateStatus(id, req.getStatus());
    }

    @PutMapping("/{id}/payment-status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Update payment status - mark CASH_ON_PICKUP as PAID (STAFF/ADMIN)")
    public OrderDtos.OrderResponse updatePaymentStatus(@PathVariable Long id,
                                                       @RequestBody java.util.Map<String, String> body) {
        String status = body.get("paymentStatus");
        return service.updatePaymentStatus(id, com.canteen.entity.PaymentStatus.valueOf(status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Cancel my order (STUDENT)")
    public ResponseEntity<Void> cancel(Authentication auth, @PathVariable Long id) {
        service.cancelOrder(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}