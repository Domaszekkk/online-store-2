package com.estore.gateway;

import lombok.Getter;

@Getter
public class GatewayException extends RuntimeException {
    private final Integer responseCode;

    public GatewayException(String message, Integer responseCode) {
        super(message);
        this.responseCode = responseCode;
    }
}
