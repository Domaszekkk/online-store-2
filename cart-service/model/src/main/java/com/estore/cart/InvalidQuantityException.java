package com.estore.cart;

public class InvalidQuantityException extends CartException {
    public InvalidQuantityException(int quantity) {
        super("Quantity must be between 1 and " + CartItem.MAXIMUM_QUANTITY + ", but was " + quantity, 400);
    }
}
