package com.canteen.entity;

import java.util.Set;

public enum OrderStatus {
    /** Online order awaiting payment. Not visible to the kitchen. */
    PENDING_PAYMENT,
    PLACED,
    ACCEPTED,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED;

    /** Orders the kitchen is working on or that are waiting to be collected. */
    public static final Set<OrderStatus> ACTIVE = Set.of(PLACED, ACCEPTED, PREPARING, READY);
    /** Orders still queued ahead of a new order, used for the wait estimate. */
    public static final Set<OrderStatus> IN_KITCHEN = Set.of(PLACED, ACCEPTED, PREPARING);

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == PLACED || next == CANCELLED;
            case PLACED    -> next == ACCEPTED || next == CANCELLED;
            case ACCEPTED  -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == READY     || next == CANCELLED;
            case READY     -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    /** Students can only back out before the kitchen starts cooking. */
    public boolean cancellableByStudent() {
        return this == PENDING_PAYMENT || this == PLACED || this == ACCEPTED;
    }
}
