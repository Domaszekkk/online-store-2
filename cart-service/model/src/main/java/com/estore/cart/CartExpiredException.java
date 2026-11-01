package com.estore.cart;

import java.util.UUID;

public class CartExpiredException extends CartException {
    public CartExpiredException(UUID cartId) {
        super("Cart with id " + cartId + " has expired because of inactivity, create a new cart", 409);
    }
}
