package com.estore.cart;

import lombok.Getter;

@Getter
public class CartException extends RuntimeException {
    private final Integer responseCode;

    public CartException(String message, Integer responseCode) {
        super(message);
        this.responseCode = responseCode;
    }
}
