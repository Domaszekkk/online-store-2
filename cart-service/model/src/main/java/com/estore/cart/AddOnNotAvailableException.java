package com.estore.cart;

import java.util.UUID;

public class AddOnNotAvailableException extends CartException {
    public AddOnNotAvailableException(UUID addOnProductId) {
        super("Add-on product with id " + addOnProductId + " is not available for purchase", 409);
    }
}
