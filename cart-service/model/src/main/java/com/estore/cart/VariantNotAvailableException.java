package com.estore.cart;

import java.util.UUID;

public class VariantNotAvailableException extends CartException {
    public VariantNotAvailableException(UUID variantId) {
        super("Variant with id " + variantId + " is not available for purchase", 409);
    }
}
