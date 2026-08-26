package com.canteen.controller;

import com.canteen.dto.PaymentDtos;
import com.canteen.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment flow")
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create a Razorpay order for an existing canteen order")
    public ResponseEntity<PaymentDtos.RazorpayOrderResponse> createOrder(
            @Valid @RequestBody PaymentDtos.CreateOrderRequest req) {
        return ResponseEntity.status(201).body(service.createOrder(req));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Verify Razorpay payment signature and mark order PAID")
    public PaymentDtos.PaymentResponse verify(@Valid @RequestBody PaymentDtos.VerifyPaymentRequest req) {
        return service.verify(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by id")
    public PaymentDtos.PaymentResponse get(@PathVariable Long id) {
        return service.get(id);
    }
}