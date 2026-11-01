package com.estore.order;

import java.util.UUID;

public class InvalidStatusChangeException extends OrderException {
    public InvalidStatusChangeException(UUID orderId, OrderStatus currentStatus, OrderStatus targetStatus) {
        super("Order " + orderId + " cannot change status from " + currentStatus + " to " + targetStatus, 409);
    }
}
