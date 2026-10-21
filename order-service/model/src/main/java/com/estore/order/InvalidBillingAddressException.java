package com.estore.order;

public class InvalidBillingAddressException extends OrderException {
    public InvalidBillingAddressException(String reason) {
        super("Invalid billing address: " + reason, 400);
    }
}
