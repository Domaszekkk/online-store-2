package com.estore.cart;

import java.util.UUID;

public class CartItemNotFoundException extends CartException {
    public CartItemNotFoundException(UUID itemId) {
        super("Cart item with id " + itemId + " not found in the cart", 404);
    }
}
