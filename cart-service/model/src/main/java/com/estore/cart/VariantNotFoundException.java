package com.estore.cart;

import java.util.UUID;

public class VariantNotFoundException extends CartException {
    public VariantNotFoundException(UUID variantId) {
        super("Variant with id " + variantId + " not found for the given product", 404);
    }
}
