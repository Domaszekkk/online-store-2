package com.estore.cart;

public class ProductCatalogUnavailableException extends CartException {
    public ProductCatalogUnavailableException() {
        super("Product service is currently unavailable, please try again later", 503);
    }
}
