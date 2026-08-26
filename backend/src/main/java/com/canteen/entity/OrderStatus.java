package com.canteen.entity;

public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PLACED    -> next == ACCEPTED || next == CANCELLED;
            case ACCEPTED  -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == READY     || next == CANCELLED;
            case READY     -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}