package com.estore.cart;

public class InvalidQuantityException extends CartException {
    public InvalidQuantityException(int quantity) {
        super("Quantity must be at least 1, but was " + quantity, 400);
    }
}
