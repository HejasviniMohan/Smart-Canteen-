package com.canteen.pickup;

import com.canteen.dto.OrderDtos;
import com.canteen.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pickup")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
@Tag(name = "Pickup", description = "Counter verification (STAFF/ADMIN)")
public class PickupController {

    private final OrderService orders;

    @PostMapping("/lookup")
    @Operation(summary = "Find an order from a scanned QR code, or today's token as a fallback")
    public OrderDtos.OrderResponse lookup(@Valid @RequestBody OrderDtos.PickupLookupRequest req) {
        return orders.lookupForPickup(req);
    }

    @PostMapping("/{orderId}/cash-received")
    @Operation(summary = "Record cash taken for a cash-on-pickup order")
    public OrderDtos.OrderResponse cashReceived(@PathVariable Long orderId) {
        return orders.cashReceived(orderId);
    }

    @PostMapping("/{orderId}/collect")
    @Operation(summary = "Hand over the order. Atomic: succeeds exactly once")
    public OrderDtos.OrderResponse collect(Authentication auth, @PathVariable Long orderId) {
        return orders.collect(orderId, auth.getName());
    }
}
