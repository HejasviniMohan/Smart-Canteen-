package com.canteen.payment;

import com.canteen.dto.PaymentDtos;
import com.canteen.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Stands in for the provider's checkout screen. Does not exist unless the sandbox is active. */
@RestController
@RequestMapping("/api/payments/sandbox")
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "sandbox")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Simulated provider (sandbox only)")
public class SandboxCheckoutController {

    private final SandboxGateway gateway;
    private final PaymentService payments;

    @PostMapping("/pay")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Simulate the student completing or failing the provider's checkout")
    public ResponseEntity<PaymentDtos.SandboxPayResponse> pay(Authentication auth,
                                                              @Valid @RequestBody PaymentDtos.SandboxPayRequest req) {
        String gatewayOrderId = payments.gatewayOrderIdForOwner(auth.getName(), req.getPaymentId());
        if (!req.getSuccess()) {
            payments.markFailed(auth.getName(), req.getPaymentId());
            return ResponseEntity.status(402).build();
        }
        String gatewayPaymentId = "sandbox_pay_" + UUID.randomUUID();
        return ResponseEntity.ok(PaymentDtos.SandboxPayResponse.builder()
                .razorpayOrderId(gatewayOrderId)
                .razorpayPaymentId(gatewayPaymentId)
                .razorpaySignature(gateway.signCheckout(gatewayOrderId, gatewayPaymentId))
                .build());
    }
}
