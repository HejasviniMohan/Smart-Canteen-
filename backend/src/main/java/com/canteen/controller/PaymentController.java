package com.canteen.controller;

import com.canteen.dto.PaymentDtos;
import com.canteen.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Online payment flow")
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Start or retry payment for one of my orders")
    public ResponseEntity<PaymentDtos.CheckoutResponse> createOrder(
            Authentication auth, @Valid @RequestBody PaymentDtos.CreateOrderRequest req) {
        return ResponseEntity.status(201).body(service.createOrder(auth.getName(), req.getOrderId()));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Verify the provider's signature and mark my order paid")
    public PaymentDtos.PaymentResponse verify(Authentication auth,
                                              @Valid @RequestBody PaymentDtos.VerifyPaymentRequest req) {
        return service.verify(auth.getName(), req);
    }

    /** The raw body is required: the signature is computed over the exact bytes received. */
    @PostMapping("/webhook")
    @Operation(summary = "Provider webhook, authenticated by HMAC signature")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                        @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        service.handleWebhook(rawBody, signature, eventId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my payments (STAFF/ADMIN can read any)")
    public PaymentDtos.PaymentResponse get(Authentication auth, @PathVariable Long id) {
        return service.get(id, auth.getName(), OrderController.isStaff(auth));
    }
}
