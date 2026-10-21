package com.estore.order;

public class CartServiceUnavailableException extends OrderException {
    public CartServiceUnavailableException() {
        super("Cart service is currently unavailable, please try again later", 503);
    }
}
