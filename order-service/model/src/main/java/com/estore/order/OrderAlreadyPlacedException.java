package com.estore.order;

import java.util.UUID;

public class OrderAlreadyPlacedException extends OrderException {
    public OrderAlreadyPlacedException(UUID cartId, String orderNumber) {
        super(orderNumber == null
                ? "An order has already been placed from cart " + cartId
                : "Order " + orderNumber + " has already been placed from cart " + cartId, 409);
    }
}
