package com.estore.cart;

import java.util.UUID;

public class EmptyCartException extends CartException {
    public EmptyCartException(UUID cartId) {
        super("Cart with id " + cartId + " is empty and cannot be checked out", 409);
    }
}
