package com.estore.cart;

import java.util.UUID;

public class CartNotActiveException extends CartException {
    public CartNotActiveException(UUID cartId, CartStatus status) {
        super("Cart with id " + cartId + " is " + status + " and can no longer be modified", 409);
    }
}
