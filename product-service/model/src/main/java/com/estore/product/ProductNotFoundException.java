package com.estore.product;

import java.util.UUID;

public class ProductNotFoundException extends ProductException {
    public ProductNotFoundException(UUID id) {
        super("Product with id " + id + " not found", 404);
    }
}
