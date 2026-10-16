package com.estore.order;

import java.util.UUID;

public class OrderNotFoundException extends OrderException {
    public OrderNotFoundException(UUID id) {
        super("Order with id " + id + " not found", 404);
    }
}
