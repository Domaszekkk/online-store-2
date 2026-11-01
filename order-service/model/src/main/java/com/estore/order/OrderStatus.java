package com.estore.order;

public enum OrderStatus {
    NEW,
    CONFIRMED,
    SHIPPED,
    COMPLETED,
    CANCELLED;

    public boolean canChangeTo(OrderStatus target) {
        return switch (this) {
            case NEW -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
