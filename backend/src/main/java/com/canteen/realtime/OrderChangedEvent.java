package com.canteen.realtime;

import com.canteen.entity.Order;

/** Published inside a transaction; delivered to browsers only if that transaction commits. */
public record OrderChangedEvent(Long orderId, String ownerEmail, String tokenNumber,
                                String status, String paymentStatus) {

    public static OrderChangedEvent of(Order o) {
        return new OrderChangedEvent(o.getId(), o.getUser().getEmail(), o.getTokenNumber(),
                o.getStatus().name(), o.getPaymentStatus().name());
    }
}
