package com.estore.order;

import lombok.Getter;

@Getter
public class OrderException extends RuntimeException {
    private final Integer responseCode;

    public OrderException(String message, Integer responseCode) {
        super(message);
        this.responseCode = responseCode;
    }
}
