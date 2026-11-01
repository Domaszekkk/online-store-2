package com.estore.product;

import lombok.Getter;

@Getter
public class ProductException extends RuntimeException {
    private final Integer responseCode;

    public ProductException(String message, Integer responseCode) {
        super(message);
        this.responseCode = responseCode;
    }
}
