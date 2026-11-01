package com.estore.cart;

import java.util.UUID;

public class CartChangedException extends CartException {
    public CartChangedException(UUID cartId) {
        super("Cart with id " + cartId + " has changed since the order summary was prepared "
                + "(content, prices or availability), review the order again", 409);
    }
}
