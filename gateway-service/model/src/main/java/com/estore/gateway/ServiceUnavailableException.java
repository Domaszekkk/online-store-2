package com.estore.gateway;

public class ServiceUnavailableException extends GatewayException {
    public ServiceUnavailableException(String serviceName) {
        super("Service " + serviceName + " is currently unavailable, please try again later", 503);
    }
}
