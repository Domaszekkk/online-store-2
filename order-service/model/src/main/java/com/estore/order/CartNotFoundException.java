package com.estore.order;

import java.util.UUID;

public class CartNotFoundException extends OrderException {
    public CartNotFoundException(UUID cartId) {
        super("Cart with id " + cartId + " not found", 404);
    }
}
