package com.estore.product;

import java.util.UUID;

public class InvalidAddOnException extends ProductException {
    private InvalidAddOnException(String message) {
        super(message, 400);
    }

    public static InvalidAddOnException missingProductId() {
        return new InvalidAddOnException("Add-on must reference a product (productId is required)");
    }

    public static InvalidAddOnException productNotFound(UUID addOnProductId) {
        return new InvalidAddOnException("Add-on product with id " + addOnProductId + " does not exist");
    }

    public static InvalidAddOnException selfReference(UUID productId) {
        return new InvalidAddOnException("Product " + productId + " cannot be an add-on of itself");
    }

    public static InvalidAddOnException notAccessory(UUID addOnProductId, ProductType type) {
        return new InvalidAddOnException("Add-on product " + addOnProductId + " is of type " + type
                + ", but only ELECTRONICS products can be sold as add-ons");
    }

    public static InvalidAddOnException duplicate(UUID addOnProductId) {
        return new InvalidAddOnException("Add-on product " + addOnProductId + " is listed more than once");
    }
}
