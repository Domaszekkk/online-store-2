package com.estore.cart;

import java.util.UUID;

public interface ProductCatalogPort {
    ProductDetails getProduct(UUID productId);
}
