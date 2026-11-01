package com.estore.gateway;

public class ServiceRejectedRequestException extends GatewayException {
    public ServiceRejectedRequestException(String message, Integer responseCode) {
        super(message, responseCode);
    }
}
