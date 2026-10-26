package com.estore.order;

import java.util.UUID;

public class CartNotOrderableException extends OrderException {
    public CartNotOrderableException(UUID cartId, String reason) {
        super("Order cannot be placed from cart " + cartId + ": " + reason, 409);
    }
}
