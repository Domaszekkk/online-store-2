package com.estore.cart;

import java.util.UUID;

public class ProductNotFoundException extends CartException {
    public ProductNotFoundException(UUID productId) {
        super("Product with id " + productId + " not found in catalog", 404);
    }
}
