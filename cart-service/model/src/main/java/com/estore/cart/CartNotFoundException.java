package com.estore.cart;

import java.util.UUID;

public class CartNotFoundException extends CartException {
    public CartNotFoundException(UUID id) {
        super("Cart with id " + id + " not found", 404);
    }
}
