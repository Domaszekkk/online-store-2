package com.estore.cart;

import java.util.List;
import java.util.UUID;

public class UnavailableItemsException extends CartException {
    public UnavailableItemsException(UUID cartId, List<String> productNames) {
        super("Cart with id " + cartId + " contains products that are no longer available: "
                + String.join(", ", productNames) + "; remove them to place the order", 409);
    }
}
